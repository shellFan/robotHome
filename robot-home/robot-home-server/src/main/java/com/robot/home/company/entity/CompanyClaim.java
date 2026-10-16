package com.robot.home.company.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 企业认领申请
 * 表名：company_claim
 * <p>
 * 状态流转: PENDING -> APPROVED / REJECTED / CANCELLED
 * 防重复: 仅待审时占用可空唯一键，允许MySQL 5.6下同企业/用户最多一条待审申请
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_claim")
public class CompanyClaim extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 企业ID */
    private Long companyId;
    /** 申请人用户ID */
    private Long applicantUserId;
    /** PENDING期间等于companyId，终态置空 */
    private Long pendingCompanyId;
    /** PENDING期间等于applicantUserId，终态置空 */
    private Long pendingApplicantUserId;
    /** 申请人姓名 */
    private String applicantName;
    /** 申请人联系电话 */
    private String applicantPhone;
    /** 申请人邮箱 */
    private String applicantEmail;
    /** 申请人在企业中的职位 */
    private String applicantPosition;
    /** 认领理由 */
    private String claimReason;
    /** 证明材料URL列表(JSON数组,私有存储) */
    private String proofMaterials;
    /** 状态: PENDING/APPROVED/REJECTED/CANCELLED */
    private String status;
    /** 审核人用户ID */
    private Long reviewerUserId;
    /** 审核人姓名 */
    private String reviewerName;
    /** 审核时间 */
    private LocalDateTime reviewTime;
    /** 审核意见 */
    private String reviewComment;
}
