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
import com.robot.home.procurement.entity.ProcurementResponse;
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
            Constants.RESPONSE_WITHDRAWN, Constants.RESPONSE_REJECTED
    ));

    @Resource
    private ProcurementResponseMapper responseMapper;
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

        // 3. 校验不能响应自己的需求（如果需求发布者是企业用户）
        if (inquiry.getUserId() != null && inquiry.getUserId().equals(userId)) {
            throw new BusinessException("不能响应自己发布的采购需求");
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
            // 唯一索引 uk_procurement_company 防重复响应
            throw new BusinessException("您的企业已响应过该采购需求，不可重复提交");
        }

        log.info("企业提交采购响应: procurementId={}, companyId={}, userId={}", dto.getProcurementId(), companyId, userId);

        // 7. 通知采购需求发布者
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

        // 8. 返回VO
        ProcurementResponseVO vo = toResponseVO(response, true);
        vo.setCompanyName(company.getName());
        vo.setCompanyLogo(company.getLogo());
        return vo;
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

        // 获取需求发布者，用于隐私判断
        Inquiry inquiry = inquiryMapper.selectById(procurementId);
        Long publisherId = inquiry != null ? inquiry.getUserId() : null;
        boolean isAdmin = isAdminUser();

        // 批量查询企业信息（避免N+1）
        Set<Long> companyIds = result.getRecords().stream()
                .map(ProcurementResponse::getCompanyId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Company> companyMap = batchQueryCompanies(companyIds);

        List<ProcurementResponseVO> voList = result.getRecords().stream()
                .map(r -> {
                    boolean isMine = currentUserId != null && currentUserId.equals(r.getContactUserId());
                    boolean isPublisher = currentUserId != null && currentUserId.equals(publisherId);
                    boolean canViewDetail = isMine || isPublisher || isAdmin;

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
            // 查指定企业的响应（Controller已校验企业成员身份）
            result = responseMapper.selectPage(page,
                    Wrappers.<ProcurementResponse>lambdaQuery()
                            .eq(ProcurementResponse::getCompanyId, companyId)
                            .ne(ProcurementResponse::getStatus, Constants.RESPONSE_WITHDRAWN)
                            .orderByDesc(ProcurementResponse::getCreateTime));
        } else {
            // 查用户所有所属企业的响应
            List<Long> myCompanyIds = companyMemberService.listActiveByUserId(userId).stream()
                    .map(com.robot.home.company.entity.CompanyMember::getCompanyId)
                    .collect(Collectors.toList());
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
            throw new BusinessException("当前响应状态不可变更: " + response.getStatus());
        }

        responseMapper.update(null, Wrappers.<ProcurementResponse>lambdaUpdate()
                .eq(ProcurementResponse::getId, responseId)
                .set(ProcurementResponse::getStatus, newStatus)
                .set(ProcurementResponse::getUpdateTime, LocalDateTime.now()));

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

        // 已撤回的不可重复撤回
        if (Constants.RESPONSE_WITHDRAWN.equals(response.getStatus())) {
            throw new BusinessException("该响应已撤回");
        }

        // 已接受的不允许撤回
        if (Constants.RESPONSE_ACCEPTED.equals(response.getStatus())) {
            throw new BusinessException("已接受的响应不可撤回");
        }

        responseMapper.update(null, Wrappers.<ProcurementResponse>lambdaUpdate()
                .eq(ProcurementResponse::getId, responseId)
                .set(ProcurementResponse::getStatus, Constants.RESPONSE_WITHDRAWN)
                .set(ProcurementResponse::getUpdateTime, LocalDateTime.now()));

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
        vo.setContactUserId(response.getContactUserId());
        vo.setStatus(response.getStatus());
        vo.setRobotIds(response.getRobotIds());
        vo.setCreateTime(response.getCreateTime());
        vo.setUpdateTime(response.getUpdateTime());
        vo.setCanViewDetail(canViewDetail);

        if (canViewDetail) {
            // 授权用户可查看完整信息
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