package com.robot.home.company.service;

import com.robot.home.common.PageResult;
import com.robot.home.company.entity.CompanyProfileChange;

/**
 * 企业资料变更服务
 * <p>
 * 高风险字段(logo/website/contactPhone/contactEmail/address)修改需审核
 * 低风险字段(intro/seoTitle等)可直接更新
 */
public interface CompanyProfileChangeService {

    /**
     * 提交资料变更申请(高风险字段)
     *
     * @param companyId       企业ID
     * @param applicantUserId 申请人ID
     * @param applicantName   申请人姓名
     * @param fieldName       字段名
     * @param fieldLabel      字段中文名
     * @param oldValue        原值
     * @param newValue        新值
     * @param changeReason    变更理由
     * @return 变更申请记录
     */
    CompanyProfileChange submitChange(Long companyId, Long applicantUserId, String applicantName,
                                      String fieldName, String fieldLabel,
                                      String oldValue, String newValue, String changeReason);

    /**
     * 直接更新低风险字段(无需审核)
     *
     * @param companyId       企业ID
     * @param applicantUserId 操作人ID
     * @param fieldName       字段名
     * @param newValue        新值
     */
    void directUpdate(Long companyId, Long applicantUserId, String fieldName, String newValue);

    /**
     * 我的变更申请列表
     */
    PageResult<CompanyProfileChange> myChanges(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 管理员: 按状态查询变更申请
     */
    PageResult<CompanyProfileChange> listByStatus(String status, Integer pageNum, Integer pageSize);

    /**
     * 管理员: 审核变更申请
     */
    CompanyProfileChange reviewChange(Long changeId, Long reviewerUserId, String reviewerName,
                                      boolean approved, String reviewComment);

    /**
     * 判断字段是否为高风险字段(需审核)
     */
    boolean isHighRiskField(String fieldName);
}