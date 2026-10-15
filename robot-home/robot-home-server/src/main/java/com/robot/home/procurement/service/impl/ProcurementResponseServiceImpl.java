package com.robot.home.procurement.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.procurement.dto.ProcurementResponseDTO;
import com.robot.home.procurement.entity.ProcurementFollowRecord;
import com.robot.home.procurement.entity.ProcurementResponse;
import com.robot.home.procurement.mapper.ProcurementFollowRecordMapper;
import com.robot.home.procurement.mapper.ProcurementResponseMapper;
import com.robot.home.procurement.service.ProcurementResponseService;
import com.robot.home.procurement.vo.ProcurementResponseVO;
import com.robot.home.message.service.NotificationService;
import com.robot.home.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采购需求响应服务实现
 */
@Slf4j
@Service
public class ProcurementResponseServiceImpl implements ProcurementResponseService {

    /** 合法的响应状态（不含WITHDRAWN，撤回走专用接口） */
    private static final List<String> VALID_UPDATE_STATUSES = Arrays.asList(
            Constants.RESPONSE_VIEWED, Constants.RESPONSE_CONTACTED,
            Constants.RESPONSE_ACCEPTED, Constants.RESPONSE_REJECTED
    );

    /** 终态状态，不可再变更 */
    private static final Set<String> TERMINAL_STATUSES = new HashSet<>(Arrays.asList(
            Constants.RESPONSE_ACCEPTED, Constants.RESPONSE_REJECTED, Constants.RESPONSE_WITHDRAWN
    ));

    @Resource
    private ProcurementResponseMapper responseMapper;
    @Resource
    private ProcurementFollowRecordMapper followRecordMapper;
    @Resource
    private InquiryMapper inquiryMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private NotificationService notificationService;
    @Resource
    private CompanyMemberService companyMemberService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcurementResponseVO submitResponse(Long userId, Long companyId, ProcurementResponseDTO dto) {
        // 1. 校验采购需求存在
        Inquiry inquiry = inquiryMapper.selectById(dto.getProcurementId());
        if (inquiry == null) {
            throw new BusinessException("采购需求不存在");
        }

        // 2. 校验企业存在
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("企业不存在");
        }
        companyMemberService.requireActiveMember(userId, companyId);
        requireVerifiedActiveCompany(company);

        // 3. 校验不能响应自己的需求（如果需求发布者是企业用户）
        if (inquiry.getUserId() != null && inquiry.getUserId().equals(userId)) {
            throw new BusinessException("不能响应自己发布的采购需求");
        }

        // 以采购需求+企业为业务幂等键，重试返回已有资源且不重复推进CRM。
        ProcurementResponse existing = findResponse(dto.getProcurementId(), companyId);
        if (existing != null) {
            return responseWithCompany(existing, company);
        }

        // 4. XSS防护
        String solution = XssUtils.escapeText(dto.getSolution());
        String priceDescription = XssUtils.escapeText(dto.getPriceDescription());
        String deliveryDescription = XssUtils.escapeText(dto.getDeliveryDescription());
        String contactDescription = XssUtils.escapeText(dto.getContactDescription());

        // 5. 生成eventKey幂等键: SHA-256(procurementId + companyId)
        String eventKey = SecureUtil.sha256(dto.getProcurementId() + "_" + companyId);

        // 6. 创建响应记录
        ProcurementResponse response = new ProcurementResponse();
        response.setProcurementId(dto.getProcurementId());
        response.setCompanyId(companyId);
        response.setContactUserId(userId);
        response.setSolution(solution);
        response.setRobotIds(dto.getRobotIds());
        response.setPriceDescription(priceDescription);
        response.setDeliveryDescription(deliveryDescription);
        response.setContactDescription(contactDescription);
        response.setStatus(Constants.RESPONSE_SUBMITTED);
        response.setEventKey(eventKey);

        try {
            responseMapper.insert(response);
        } catch (DuplicateKeyException e) {
            // 并发重试由唯一索引收敛，返回已有资源。
            ProcurementResponse concurrent = findResponse(dto.getProcurementId(), companyId);
            if (concurrent != null) {
                return responseWithCompany(concurrent, company);
            }
            throw e;
        }

        log.info("企业提交采购响应: procurementId={}, companyId={}, userId={}", dto.getProcurementId(), companyId, userId);

        // 7. 同一事务内推进CRM并写入一条可追溯记录。
        advancePipelineAndRecordResponse(inquiry, response, userId, company.getName());

        // 8. 通知采购需求发布者
        try {
            if (inquiry.getUserId() != null) {
                notificationService.sendNotification(
                        inquiry.getUserId(),
                        Constants.NOTIFY_PROCUREMENT_RESPONSE,
                        "企业响应了您的采购需求",
                        company.getName() + "对您的采购需求提交了方案",
                        "PROCUREMENT", dto.getProcurementId(),
                        "procurement_response:" + response.getId());
            }
        } catch (Exception e) {
            log.warn("采购响应通知失败(不影响响应提交): procurementId={}, error={}", dto.getProcurementId(), e.getMessage());
        }

        // 9. 返回VO
        return responseWithCompany(response, company);
    }

    @Override
    public PageResult<ProcurementResponseVO> listByProcurement(Long procurementId, Long currentUserId,
                                                                 Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<ProcurementResponse> page = new Page<>(pn, ps);

        IPage<ProcurementResponse> result = responseMapper.selectPage(page,
                Wrappers.<ProcurementResponse>lambdaQuery()
                        .eq(ProcurementResponse::getProcurementId, procurementId)
                        .ne(ProcurementResponse::getStatus, Constants.RESPONSE_WITHDRAWN)
                        .orderByDesc(ProcurementResponse::getCreateTime));

        // 只有需求发布者、后台管理员或响应企业的有效成员可查看列表。
        Inquiry inquiry = inquiryMapper.selectById(procurementId);
        if (inquiry == null) {
            throw new BusinessException("采购需求不存在");
        }
        Long publisherId = inquiry.getUserId();
        boolean isAdmin = isAdminUser();
        Set<Long> myVerifiedCompanyIds = verifiedCompanyIdsForUser(currentUserId);
        boolean isPublisher = currentUserId != null && currentUserId.equals(publisherId);
        boolean isRespondingMember = !myVerifiedCompanyIds.isEmpty()
                && responseMapper.selectCount(Wrappers.<ProcurementResponse>lambdaQuery()
                .eq(ProcurementResponse::getProcurementId, procurementId)
                .in(ProcurementResponse::getCompanyId, myVerifiedCompanyIds)) > 0;
        if (!isPublisher && !isAdmin && !isRespondingMember) {
            throw new BusinessException("无权查看该采购需求的响应");
        }

        // 批量查询企业信息（避免N+1）
        Set<Long> companyIds = result.getRecords().stream()
                .map(ProcurementResponse::getCompanyId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Company> companyMap = batchQueryCompanies(companyIds);

        List<ProcurementResponseVO> voList = result.getRecords().stream()
                .map(r -> {
                    boolean isMine = currentUserId != null && currentUserId.equals(r.getContactUserId());
                    boolean canViewDetail = isPublisher || isAdmin || myVerifiedCompanyIds.contains(r.getCompanyId());

                    ProcurementResponseVO vo = toResponseVO(r, canViewDetail);
                    Company company = companyMap.get(r.getCompanyId());
                    if (company != null) {
                        vo.setCompanyName(company.getName());
                        vo.setCompanyLogo(company.getLogo());
                    }
                    vo.setIsMine(isMine);
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    @Override
    public PageResult<ProcurementResponseVO> listByCompany(Long companyId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<ProcurementResponse> page = new Page<>(pn, ps);

        IPage<ProcurementResponse> result = responseMapper.selectPage(page,
                Wrappers.<ProcurementResponse>lambdaQuery()
                        .eq(ProcurementResponse::getCompanyId, companyId)
                        .ne(ProcurementResponse::getStatus, Constants.RESPONSE_WITHDRAWN)
                        .orderByDesc(ProcurementResponse::getCreateTime));

        List<ProcurementResponseVO> voList = result.getRecords().stream()
                .map(r -> toResponseVO(r, true))
                .collect(Collectors.toList());

        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    @Override
    public PageResult<ProcurementResponseVO> listMyResponses(Long userId, Long companyId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<ProcurementResponse> page = new Page<>(pn, ps);

        IPage<ProcurementResponse> result;
        if (companyId != null) {
            requireVerifiedActiveMember(userId, companyId);
            result = responseMapper.selectPage(page,
                    Wrappers.<ProcurementResponse>lambdaQuery()
                            .eq(ProcurementResponse::getCompanyId, companyId)
                            .ne(ProcurementResponse::getStatus, Constants.RESPONSE_WITHDRAWN)
                            .orderByDesc(ProcurementResponse::getCreateTime));
        } else {
            // 查用户所有所属企业的响应
            List<Long> myCompanyIds = new java.util.ArrayList<>(verifiedCompanyIdsForUser(userId));
            if (myCompanyIds.isEmpty()) {
                return PageResult.of(pn, ps, 0, new java.util.ArrayList<>());
            }
            result = responseMapper.selectPage(page,
                    Wrappers.<ProcurementResponse>lambdaQuery()
                            .in(ProcurementResponse::getCompanyId, myCompanyIds)
                            .ne(ProcurementResponse::getStatus, Constants.RESPONSE_WITHDRAWN)
                            .orderByDesc(ProcurementResponse::getCreateTime));
        }

        List<ProcurementResponseVO> voList = result.getRecords().stream()
                .map(r -> toResponseVO(r, true))
                .collect(Collectors.toList());

        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long responseId, String newStatus, Long userId) {
        if (!VALID_UPDATE_STATUSES.contains(newStatus)) {
            throw new BusinessException("不合法的响应状态: " + newStatus);
        }

        ProcurementResponse response = responseMapper.selectById(responseId);
        if (response == null) {
            throw new BusinessException("响应记录不存在");
        }

        // IDOR防护: 校验权限 - 需求发布者或管理员可以更新状态
        Inquiry inquiry = inquiryMapper.selectById(response.getProcurementId());
        if (inquiry == null) {
            throw new BusinessException("关联的采购需求不存在");
        }

        boolean isPublisher = userId.equals(inquiry.getUserId());
        boolean isAdmin = isAdminUser();
        if (!isPublisher && !isAdmin) {
            throw new BusinessException("无权修改该响应状态");
        }

        // 终态不可变更
        if (TERMINAL_STATUSES.contains(response.getStatus())) {
            if (response.getStatus().equals(newStatus)) {
                return;
            }
            throw new BusinessException("当前响应状态不可变更: " + response.getStatus());
        }

        if (response.getStatus().equals(newStatus)) {
            return;
        }
        if (!canTransitionResponse(response.getStatus(), newStatus)) {
            throw new BusinessException("不允许的响应状态变更: " + response.getStatus() + " -> " + newStatus);
        }

        int updated = responseMapper.update(null, Wrappers.<ProcurementResponse>lambdaUpdate()
                .eq(ProcurementResponse::getId, responseId)
                .eq(ProcurementResponse::getStatus, response.getStatus())
                .set(ProcurementResponse::getStatus, newStatus)
                .set(ProcurementResponse::getUpdateTime, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException("响应状态已被其他操作修改，请刷新后重试");
        }

        log.info("响应状态更新: responseId={}, {} → {}, operator={}", responseId, response.getStatus(), newStatus, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawResponse(Long responseId, Long userId) {
        ProcurementResponse response = responseMapper.selectById(responseId);
        if (response == null) {
            throw new BusinessException("响应记录不存在");
        }

        // IDOR防护: 只有响应提交者本人可以撤回
        if (!userId.equals(response.getContactUserId())) {
            throw new BusinessException("只能撤回自己提交的响应");
        }
        requireVerifiedActiveMember(userId, response.getCompanyId());

        // 已撤回的不可重复撤回
        if (Constants.RESPONSE_WITHDRAWN.equals(response.getStatus())) {
            throw new BusinessException("该响应已撤回");
        }

        if (TERMINAL_STATUSES.contains(response.getStatus())) {
            throw new BusinessException("当前响应状态不可撤回: " + response.getStatus());
        }

        int updated = responseMapper.update(null, Wrappers.<ProcurementResponse>lambdaUpdate()
                .eq(ProcurementResponse::getId, responseId)
                .eq(ProcurementResponse::getStatus, response.getStatus())
                .set(ProcurementResponse::getStatus, Constants.RESPONSE_WITHDRAWN)
                .set(ProcurementResponse::getUpdateTime, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException("响应状态已被其他操作修改，请刷新后重试");
        }

        log.info("响应撤回: responseId={}, userId={}", responseId, userId);
    }

    /**
     * Entity → VO 转换
     *
     * @param response     实体
     * @param canViewDetail 是否可查看完整信息（隐私控制）
     */
    private ProcurementResponseVO toResponseVO(ProcurementResponse response, boolean canViewDetail) {
        ProcurementResponseVO vo = new ProcurementResponseVO();
        vo.setId(response.getId());
        vo.setProcurementId(response.getProcurementId());
        vo.setCompanyId(response.getCompanyId());
        vo.setStatus(response.getStatus());
        vo.setCreateTime(response.getCreateTime());
        vo.setUpdateTime(response.getUpdateTime());
        vo.setCanViewDetail(canViewDetail);

        if (canViewDetail) {
            // 授权用户可查看完整信息
            vo.setContactUserId(response.getContactUserId());
            vo.setRobotIds(response.getRobotIds());
            vo.setSolution(response.getSolution());
            vo.setPriceDescription(response.getPriceDescription());
            vo.setDeliveryDescription(response.getDeliveryDescription());
            vo.setContactDescription(response.getContactDescription());
        } else {
            // 非授权用户脱敏：只展示摘要
            vo.setSolution(maskField(response.getSolution(), 50));
            vo.setPriceDescription(maskField(response.getPriceDescription(), 30));
            // 交付说明和联系方式不展示
        }
        return vo;
    }

    /**
     * 字段脱敏：截取前maxLen字符 + "..."
     */
    private String maskField(String value, int maxLen) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }

    private ProcurementResponse findResponse(Long procurementId, Long companyId) {
        return responseMapper.selectOne(Wrappers.<ProcurementResponse>lambdaQuery()
                .eq(ProcurementResponse::getProcurementId, procurementId)
                .eq(ProcurementResponse::getCompanyId, companyId));
    }

    private ProcurementResponseVO responseWithCompany(ProcurementResponse response, Company company) {
        ProcurementResponseVO vo = toResponseVO(response, true);
        vo.setCompanyName(company.getName());
        vo.setCompanyLogo(company.getLogo());
        return vo;
    }

    private void requireVerifiedActiveMember(Long userId, Long companyId) {
        companyMemberService.requireActiveMember(userId, companyId);
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("企业不存在");
        }
        requireVerifiedActiveCompany(company);
    }

    private void requireVerifiedActiveCompany(Company company) {
        if (!Integer.valueOf(1).equals(company.getStatus())
                || !"VERIFIED".equals(company.getVerificationStatus())) {
            throw new BusinessException("只有正常且已认证的企业可操作采购响应");
        }
    }

    private Set<Long> verifiedCompanyIdsForUser(Long userId) {
        if (userId == null) {
            return new HashSet<>();
        }
        List<Long> companyIds = companyMemberService.listActiveByUserId(userId).stream()
                .map(com.robot.home.company.entity.CompanyMember::getCompanyId)
                .collect(Collectors.toList());
        if (companyIds.isEmpty()) {
            return new HashSet<>();
        }
        return companyMapper.selectBatchIds(companyIds).stream()
                .filter(c -> Integer.valueOf(1).equals(c.getStatus()))
                .filter(c -> "VERIFIED".equals(c.getVerificationStatus()))
                .map(Company::getId)
                .collect(Collectors.toSet());
    }

    private void advancePipelineAndRecordResponse(Inquiry inquiry, ProcurementResponse response,
                                                   Long operatorId, String operatorName) {
        String oldStatus = inquiry.getPipelineStatus() == null ? Constants.PIPELINE_NEW : inquiry.getPipelineStatus();
        if (Constants.PIPELINE_WON.equals(oldStatus) || Constants.PIPELINE_LOST.equals(oldStatus)
                || Constants.PIPELINE_CLOSED.equals(oldStatus)) {
            throw new BusinessException("采购线索已进入终态，不可提交新响应");
        }

        boolean shouldAdvance = Constants.PIPELINE_NEW.equals(oldStatus)
                || Constants.PIPELINE_CONTACTED.equals(oldStatus)
                || Constants.PIPELINE_QUALIFIED.equals(oldStatus)
                || Constants.PIPELINE_MATCHING.equals(oldStatus);
        String newStatus = oldStatus;
        if (shouldAdvance) {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Inquiry> update =
                    Wrappers.<Inquiry>lambdaUpdate().eq(Inquiry::getId, inquiry.getId());
            if (inquiry.getPipelineStatus() == null) {
                update.isNull(Inquiry::getPipelineStatus);
            } else {
                update.eq(Inquiry::getPipelineStatus, inquiry.getPipelineStatus());
            }
            update.set(Inquiry::getPipelineStatus, Constants.PIPELINE_RESPONDED)
                    .set(Inquiry::getUpdateTime, LocalDateTime.now());
            if (inquiryMapper.update(null, update) != 1) {
                throw new BusinessException("CRM状态已被其他操作修改，请重试");
            }
            newStatus = Constants.PIPELINE_RESPONDED;
        }

        ProcurementFollowRecord record = new ProcurementFollowRecord();
        record.setProcurementId(inquiry.getId());
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setAction(shouldAdvance ? Constants.CRM_ACTION_STATUS_CHANGE : Constants.CRM_ACTION_FOLLOW_UP);
        record.setContent(shouldAdvance
                ? "企业响应触发Pipeline状态变更: " + oldStatus + " -> " + newStatus
                : "企业提交采购响应: responseId=" + response.getId());
        record.setOldStatus(oldStatus);
        record.setNewStatus(newStatus);
        record.setCreateTime(LocalDateTime.now());
        followRecordMapper.insert(record);
    }

    private boolean canTransitionResponse(String currentStatus, String newStatus) {
        if (Constants.RESPONSE_SUBMITTED.equals(currentStatus)) {
            return Constants.RESPONSE_VIEWED.equals(newStatus)
                    || Constants.RESPONSE_CONTACTED.equals(newStatus)
                    || Constants.RESPONSE_ACCEPTED.equals(newStatus)
                    || Constants.RESPONSE_REJECTED.equals(newStatus);
        }
        if (Constants.RESPONSE_VIEWED.equals(currentStatus)) {
            return Constants.RESPONSE_CONTACTED.equals(newStatus)
                    || Constants.RESPONSE_ACCEPTED.equals(newStatus)
                    || Constants.RESPONSE_REJECTED.equals(newStatus);
        }
        if (Constants.RESPONSE_CONTACTED.equals(currentStatus)) {
            return Constants.RESPONSE_ACCEPTED.equals(newStatus)
                    || Constants.RESPONSE_REJECTED.equals(newStatus);
        }
        return false;
    }

    /**
     * 批量查询企业信息（避免N+1）
     */
    private Map<Long, Company> batchQueryCompanies(Set<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Company> companies = companyMapper.selectBatchIds(companyIds);
        return companies.stream().collect(Collectors.toMap(Company::getId, c -> c));
    }

    /**
     * 判断当前用户是否为管理员
     */
    private boolean isAdminUser() {
        String role = UserContext.getRole();
        return Constants.ROLE_ADMIN.equals(role);
    }
}
