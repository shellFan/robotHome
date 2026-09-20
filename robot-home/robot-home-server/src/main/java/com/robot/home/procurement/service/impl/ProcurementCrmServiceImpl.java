package com.robot.home.procurement.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.SensitiveUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.procurement.entity.ProcurementFollowRecord;
import com.robot.home.procurement.entity.ProcurementResponse;
import com.robot.home.procurement.mapper.ProcurementFollowRecordMapper;
import com.robot.home.procurement.mapper.ProcurementResponseMapper;
import com.robot.home.procurement.service.ProcurementCrmService;
import com.robot.home.procurement.vo.ProcurementCrmVO;
import com.robot.home.procurement.vo.ProcurementResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购CRM服务实现
 */
@Slf4j
@Service
public class ProcurementCrmServiceImpl implements ProcurementCrmService {

    /** 合法的Pipeline状态 */
    private static final List<String> VALID_PIPELINE_STATUSES = Arrays.asList(
            Constants.PIPELINE_NEW, Constants.PIPELINE_CONTACTED, Constants.PIPELINE_QUALIFIED,
            Constants.PIPELINE_MATCHING, Constants.PIPELINE_RESPONDED, Constants.PIPELINE_NEGOTIATING,
            Constants.PIPELINE_WON, Constants.PIPELINE_LOST, Constants.PIPELINE_CLOSED
    );

    /** 合法的CRM跟进操作 */
    private static final List<String> VALID_CRM_ACTIONS = Arrays.asList(
            Constants.CRM_ACTION_STATUS_CHANGE, Constants.CRM_ACTION_FOLLOW_UP,
            Constants.CRM_ACTION_CALL, Constants.CRM_ACTION_EMAIL,
            Constants.CRM_ACTION_MEETING, Constants.CRM_ACTION_NOTE,
            Constants.CRM_ACTION_ASSIGN
    );

    @Resource
    private InquiryMapper inquiryMapper;
    @Resource
    private ProcurementFollowRecordMapper followRecordMapper;
    @Resource
    private ProcurementResponseMapper responseMapper;
    @Resource
    private CompanyMapper companyMapper;

    @Override
    public PageResult<ProcurementCrmVO> crmList(String pipelineStatus, Long crmOwner, Integer crmPriority,
                                                  String keyword, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Inquiry> page = new Page<>(pn, ps);

        IPage<Inquiry> result = inquiryMapper.selectPage(page, Wrappers.<Inquiry>lambdaQuery()
                .eq(StrUtil.isNotBlank(pipelineStatus), Inquiry::getPipelineStatus, pipelineStatus)
                .eq(crmOwner != null, Inquiry::getCrmOwner, crmOwner)
                .eq(crmPriority != null, Inquiry::getCrmPriority, crmPriority)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .likeRight(Inquiry::getCompanyName, keyword)
                        .or().likeRight(Inquiry::getName, keyword)
                        .or().likeRight(Inquiry::getRobotName, keyword))
                .isNotNull(Inquiry::getInquiryType)  // 只查采购类型
                .orderByDesc(Inquiry::getUpdateTime));

        List<ProcurementCrmVO> voList = result.getRecords().stream()
                .map(this::toCrmVO)
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    @Override
    public ProcurementCrmVO crmDetail(Long procurementId) {
        Inquiry inquiry = inquiryMapper.selectById(procurementId);
        if (inquiry == null) {
            throw new BusinessException("采购需求不存在");
        }
        ProcurementCrmVO vo = toCrmVO(inquiry);

        // 填充跟进记录
        List<ProcurementFollowRecord> records = listFollowRecords(procurementId);
        vo.setFollowRecords(records.stream().map(r -> {
            ProcurementCrmVO.FollowRecordVO frv = new ProcurementCrmVO.FollowRecordVO();
            frv.setId(r.getId());
            frv.setProcurementId(r.getProcurementId());
            frv.setOperatorName(r.getOperatorName());
            frv.setAction(r.getAction());
            frv.setContent(r.getContent());
            frv.setOldStatus(r.getOldStatus());
            frv.setNewStatus(r.getNewStatus());
            frv.setNextFollowTime(r.getNextFollowTime());
            frv.setCreateTime(r.getCreateTime());
            return frv;
        }).collect(Collectors.toList()));

        // 填充响应列表（管理员视角，不脱敏）
        List<ProcurementResponse> responses = responseMapper.selectList(
                Wrappers.<ProcurementResponse>lambdaQuery()
                        .eq(ProcurementResponse::getProcurementId, procurementId)
                        .orderByDesc(ProcurementResponse::getCreateTime));
        vo.setResponses(responses.stream().map(r -> {
            ProcurementResponseVO rvo = new ProcurementResponseVO();
            rvo.setId(r.getId());
            rvo.setProcurementId(r.getProcurementId());
            rvo.setCompanyId(r.getCompanyId());
            rvo.setContactUserId(r.getContactUserId());
            rvo.setSolution(r.getSolution());
            rvo.setRobotIds(r.getRobotIds());
            rvo.setPriceDescription(r.getPriceDescription());
            rvo.setDeliveryDescription(r.getDeliveryDescription());
            rvo.setContactDescription(r.getContactDescription());
            rvo.setStatus(r.getStatus());
            rvo.setCreateTime(r.getCreateTime());
            rvo.setUpdateTime(r.getUpdateTime());
            rvo.setCanViewDetail(true);
            // 批量填充企业名（避免N+1，此处响应量通常较少）
            if (r.getCompanyId() != null) {
                Company company = companyMapper.selectById(r.getCompanyId());
                if (company != null) {
                    rvo.setCompanyName(company.getName());
                    rvo.setCompanyLogo(company.getLogo());
                }
            }
            return rvo;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePipelineStatus(Long procurementId, String newPipelineStatus, Long operatorId, String operatorName) {
        if (!VALID_PIPELINE_STATUSES.contains(newPipelineStatus)) {
            throw new BusinessException("不合法的Pipeline状态: " + newPipelineStatus);
        }
        Inquiry inquiry = inquiryMapper.selectById(procurementId);
        if (inquiry == null) {
            throw new BusinessException("采购需求不存在");
        }

        String oldStatus = inquiry.getPipelineStatus();
        inquiryMapper.update(null, new LambdaUpdateWrapper<Inquiry>()
                .eq(Inquiry::getId, procurementId)
                .set(Inquiry::getPipelineStatus, newPipelineStatus)
                .set(Inquiry::getUpdateTime, LocalDateTime.now()));

        // 记录跟进
        ProcurementFollowRecord record = new ProcurementFollowRecord();
        record.setProcurementId(procurementId);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setAction(Constants.CRM_ACTION_STATUS_CHANGE);
        record.setContent("Pipeline状态变更: " + oldStatus + " → " + newPipelineStatus);
        record.setOldStatus(oldStatus);
        record.setNewStatus(newPipelineStatus);
        record.setCreateTime(LocalDateTime.now());
        followRecordMapper.insert(record);

        log.info("Pipeline状态更新: procurementId={}, {} → {}, operator={}", procurementId, oldStatus, newPipelineStatus, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignOwner(Long procurementId, Long crmOwner, Long operatorId, String operatorName) {
        Inquiry inquiry = inquiryMapper.selectById(procurementId);
        if (inquiry == null) {
            throw new BusinessException("采购需求不存在");
        }

        Long oldOwner = inquiry.getCrmOwner();
        inquiryMapper.update(null, new LambdaUpdateWrapper<Inquiry>()
                .eq(Inquiry::getId, procurementId)
                .set(Inquiry::getCrmOwner, crmOwner)
                .set(Inquiry::getUpdateTime, LocalDateTime.now()));

        // 记录跟进
        ProcurementFollowRecord record = new ProcurementFollowRecord();
        record.setProcurementId(procurementId);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setAction(Constants.CRM_ACTION_ASSIGN);
        record.setContent("分配负责人: " + oldOwner + " → " + crmOwner);
        record.setCreateTime(LocalDateTime.now());
        followRecordMapper.insert(record);

        log.info("CRM负责人分配: procurementId={}, owner={}, operator={}", procurementId, crmOwner, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcurementFollowRecord addFollowRecord(Long procurementId, String action, String content,
                                                     LocalDateTime nextFollowTime,
                                                     Long operatorId, String operatorName) {
        if (!VALID_CRM_ACTIONS.contains(action)) {
            throw new BusinessException("不支持的CRM操作类型: " + action);
        }
        Inquiry inquiry = inquiryMapper.selectById(procurementId);
        if (inquiry == null) {
            throw new BusinessException("采购需求不存在");
        }

        String escapedContent = XssUtils.escapeText(content);

        ProcurementFollowRecord record = new ProcurementFollowRecord();
        record.setProcurementId(procurementId);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setAction(action);
        record.setContent(escapedContent);
        record.setNextFollowTime(nextFollowTime);
        record.setCreateTime(LocalDateTime.now());
        followRecordMapper.insert(record);

        // 更新最后跟进时间，如有下次跟进时间也一并更新
        LambdaUpdateWrapper<Inquiry> updateWrapper = new LambdaUpdateWrapper<Inquiry>()
                .eq(Inquiry::getId, procurementId)
                .set(Inquiry::getLastFollowTime, LocalDateTime.now())
                .set(Inquiry::getUpdateTime, LocalDateTime.now());
        if (nextFollowTime != null) {
            updateWrapper.set(Inquiry::getNextFollowTime, nextFollowTime);
        }
        inquiryMapper.update(null, updateWrapper);

        log.info("添加CRM跟进记录: procurementId={}, action={}, operator={}", procurementId, action, operatorId);
        return record;
    }

    @Override
    public List<ProcurementFollowRecord> listFollowRecords(Long procurementId) {
        return followRecordMapper.selectList(Wrappers.<ProcurementFollowRecord>lambdaQuery()
                .eq(ProcurementFollowRecord::getProcurementId, procurementId)
                .orderByDesc(ProcurementFollowRecord::getCreateTime));
    }

    @Override
    public Map<String, Long> pipelineStats() {
        Map<String, Long> stats = new HashMap<>();
        for (String status : VALID_PIPELINE_STATUSES) {
            Long count = inquiryMapper.selectCount(Wrappers.<Inquiry>lambdaQuery()
                    .eq(Inquiry::getPipelineStatus, status));
            stats.put(status, count);
        }
        // 统计未设置Pipeline状态的采购需求
        Long noPipeline = inquiryMapper.selectCount(Wrappers.<Inquiry>lambdaQuery()
                .isNull(Inquiry::getPipelineStatus)
                .isNotNull(Inquiry::getInquiryType));
        stats.put("NO_PIPELINE", noPipeline);
        return stats;
    }

    /**
     * Inquiry → ProcurementCrmVO 转换
     */
    private ProcurementCrmVO toCrmVO(Inquiry inquiry) {
        ProcurementCrmVO vo = new ProcurementCrmVO();
        vo.setId(inquiry.getId());
        vo.setInquiryId(inquiry.getId());
        vo.setName(inquiry.getName());
        // 管理端也做手机号脱敏（安全原则）
        vo.setPhone(SensitiveUtils.maskPhone(inquiry.getPhone()));
        vo.setEmail(SensitiveUtils.maskEmail(inquiry.getEmail()));
        vo.setCompanyName(inquiry.getCompanyName());
        vo.setRobotName(inquiry.getRobotName());
        vo.setProcurementScene(inquiry.getProcurementScene());
        vo.setUsageScene(inquiry.getUsageScene());
        vo.setRequirementType(inquiry.getRequirementType());
        vo.setBudget(inquiry.getBudget());
        vo.setQuantity(inquiry.getQuantity());
        vo.setRemark(inquiry.getRemark());
        vo.setLeadScore(inquiry.getLeadScore());
        vo.setLeadPriority(inquiry.getLeadPriority());
        vo.setPipelineStatus(inquiry.getPipelineStatus());
        vo.setCrmOwner(inquiry.getCrmOwner());
        vo.setCrmPriority(inquiry.getCrmPriority());
        vo.setNextFollowTime(inquiry.getNextFollowTime());
        vo.setLastFollowTime(inquiry.getLastFollowTime());
        vo.setCrmSource(inquiry.getCrmSource());
        vo.setCrmRemark(inquiry.getCrmRemark());
        vo.setCreateTime(inquiry.getCreateTime());
        vo.setUpdateTime(inquiry.getUpdateTime());
        return vo;
    }
}