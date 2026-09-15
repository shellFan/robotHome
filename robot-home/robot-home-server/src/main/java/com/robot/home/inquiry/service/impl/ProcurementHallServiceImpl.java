package com.robot.home.inquiry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.SensitiveUtils;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.inquiry.service.ProcurementHallService;
import com.robot.home.inquiry.vo.InquiryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 采购需求大厅服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementHallServiceImpl implements ProcurementHallService {

    private final InquiryMapper inquiryMapper;

    @Override
    public PageResult<InquiryVO> hallList(String category, String usageScene, Integer pageNum, Integer pageSize) {
        pageNum = PageUtils.normalizePageNum(pageNum);
        pageSize = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<Inquiry> wrapper = new LambdaQueryWrapper<Inquiry>()
                .eq(Inquiry::getRequirementType, "OPEN")
                .isNotNull(Inquiry::getRequirementType)
                .orderByDesc(Inquiry::getLeadPriority)
                .orderByDesc(Inquiry::getCreateTime);
        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(Inquiry::getCategory, category);
        }
        if (usageScene != null && !usageScene.trim().isEmpty()) {
            wrapper.eq(Inquiry::getUsageScene, usageScene);
        }
        Page<Inquiry> page = inquiryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(pageNum, pageSize, page.getTotal(),
                page.getRecords().stream().map(this::toHallVO).collect(Collectors.toList()));
    }

    @Override
    public InquiryVO hallDetail(Long id) {
        Inquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) {
            throw new BusinessException("需求不存在");
        }
        // 仅展示OPEN类型
        if (!"OPEN".equals(inquiry.getRequirementType())) {
            throw new BusinessException("该需求不在采购大厅中");
        }
        return toHallVO(inquiry);
    }

    @Override
    public PageResult<InquiryVO> adminList(Integer status, String requirementType, Integer pageNum, Integer pageSize) {
        pageNum = PageUtils.normalizePageNum(pageNum);
        pageSize = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<Inquiry> wrapper = new LambdaQueryWrapper<Inquiry>()
                .orderByDesc(Inquiry::getLeadPriority)
                .orderByDesc(Inquiry::getLeadScore)
                .orderByDesc(Inquiry::getCreateTime);
        if (status != null) {
            wrapper.eq(Inquiry::getStatus, status);
        }
        if (requirementType != null && !requirementType.trim().isEmpty()) {
            wrapper.eq(Inquiry::getRequirementType, requirementType);
        }
        Page<Inquiry> page = inquiryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(pageNum, pageSize, page.getTotal(),
                page.getRecords().stream().map(this::toAdminVO).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public void assign(Long inquiryId, Long adminUserId) {
        inquiryMapper.update(null, new LambdaUpdateWrapper<Inquiry>()
                .eq(Inquiry::getId, inquiryId)
                .set(Inquiry::getAssignedTo, adminUserId)
                .set(Inquiry::getStatus, 2) // 处理中
                .set(Inquiry::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void updateLeadScore(Long inquiryId, Integer score) {
        inquiryMapper.update(null, new LambdaUpdateWrapper<Inquiry>()
                .eq(Inquiry::getId, inquiryId)
                .set(Inquiry::getLeadScore, score)
                .set(Inquiry::getUpdateTime, LocalDateTime.now()));
    }

    /** 采购大厅脱敏VO */
    private InquiryVO toHallVO(Inquiry i) {
        InquiryVO vo = new InquiryVO();
        vo.setId(i.getId());
        vo.setRobotId(i.getRobotId());
        vo.setRobotName(i.getRobotName());
        vo.setRequirementType(i.getRequirementType());
        vo.setCategory(i.getCategory());
        vo.setUsageScene(i.getUsageScene());
        vo.setTechnicalRequirements(i.getTechnicalRequirements());
        vo.setNeedDemo(i.getNeedDemo());
        vo.setNeedSolution(i.getNeedSolution());
        vo.setQuantity(i.getQuantity());
        vo.setBudget(i.getBudget());
        vo.setCustomerType(i.getCustomerType());
        vo.setCustomerTypeName(Integer.valueOf(2).equals(i.getCustomerType()) ? "企业" : "个人");
        vo.setCompanyName(i.getCompanyName());
        vo.setRegion(i.getRegion());
        vo.setProcurementScene(i.getProcurementScene());
        vo.setPurchaseTime(i.getPurchaseTime());
        vo.setLeadPriority(i.getLeadPriority());
        vo.setCreateTime(i.getCreateTime());
        // 脱敏: 不暴露name/phone/email
        vo.setName(null);
        vo.setPhone(null);
        return vo;
    }

    /** 管理端完整VO */
    private InquiryVO toAdminVO(Inquiry i) {
        InquiryVO vo = new InquiryVO();
        vo.setId(i.getId());
        vo.setRobotId(i.getRobotId());
        vo.setRobotName(i.getRobotName());
        vo.setUserId(i.getUserId());
        vo.setName(i.getName());
        vo.setPhone(SensitiveUtils.maskPhone(i.getPhone()));
        vo.setRegion(i.getRegion());
        vo.setCustomerType(i.getCustomerType());
        vo.setCustomerTypeName(Integer.valueOf(2).equals(i.getCustomerType()) ? "企业" : "个人");
        vo.setCompanyName(i.getCompanyName());
        vo.setQuantity(i.getQuantity());
        vo.setBudget(i.getBudget());
        vo.setRemark(i.getRemark());
        vo.setStatus(i.getStatus());
        vo.setStatusName(statusName(i.getStatus()));
        vo.setHandleNote(i.getHandleNote());
        vo.setCreateTime(i.getCreateTime());
        vo.setInquiryType(i.getInquiryType());
        vo.setProcurementScene(i.getProcurementScene());
        vo.setPurchaseTime(i.getPurchaseTime());
        vo.setLeadPriority(i.getLeadPriority());
        vo.setLeadReason(i.getLeadReason());
        vo.setRequirementType(i.getRequirementType());
        vo.setCategory(i.getCategory());
        vo.setUsageScene(i.getUsageScene());
        vo.setTechnicalRequirements(i.getTechnicalRequirements());
        vo.setNeedDemo(i.getNeedDemo());
        vo.setNeedSolution(i.getNeedSolution());
        vo.setLeadScore(i.getLeadScore());
        vo.setAssignedTo(i.getAssignedTo());
        return vo;
    }

    private String statusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待处理";
            case 2: return "处理中";
            case 3: return "已联系";
            case 4: return "已成交";
            case 5: return "已关闭";
            default: return "未知";
        }
    }
}