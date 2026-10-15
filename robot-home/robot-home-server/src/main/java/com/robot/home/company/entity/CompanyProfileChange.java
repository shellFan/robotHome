package com.robot.home.company.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 企业资料变更申请
 * 高风险字段(logo/website/contact等)修改需审核
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_profile_change")
public class CompanyProfileChange extends BaseEntity {

    private Long companyId;
    @TableField("submitter_user_id")
    private Long applicantUserId;
    @TableField("submitter_name")
    private String applicantName;

    private String changeType;

    /** 变更字段名, 如 logo/website/contactPhone */
    private String fieldName;

    /** 变更字段中文名, 如 企业Logo/官网/联系电话 */
    private String fieldLabel;

    /** 原值 */
    private String oldValue;

    /** 新值 */
    private String newValue;

    /** 变更理由 */
    private String changeReason;

    /** PENDING/APPROVED/REJECTED/CANCELLED */
    private String status;

    private Long reviewerUserId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private String reviewComment;
}
