package com.robot.home.company.service;

import com.robot.home.common.PageResult;
import com.robot.home.company.entity.CompanyClaim;

import java.util.List;

/**
 * 企业认领服务
 * <p>
 * 认领流程:
 * 1. 用户提交认领申请(PENDING) -> company.verification_status=PENDING
 * 2. 管理员审核通过(APPROVED) -> company.verification_status=VERIFIED, 申请人成为OWNER成员
 * 3. 管理员审核拒绝(REJECTED) -> company.verification_status=REJECTED
 * 4. 用户可取消自己的PENDING申请(CANCELLED)
 * <p>
 * 安全: uk_company_pending(company_id, status, deleted)防同一企业多个PENDING申请
 */
public interface CompanyClaimService {

    /**
     * 提交企业认领申请
     *
     * @param companyId         企业ID
     * @param applicantUserId   申请人用户ID
     * @param applicantName     申请人姓名
     * @param applicantPhone    申请人电话
     * @param applicantEmail    申请人邮箱
     * @param applicantPosition 申请人职位
     * @param claimReason       认领理由
     * @param proofMaterials    证明材料(JSON数组字符串)
     * @return 认领申请记录
     */
    CompanyClaim submitClaim(Long companyId, Long applicantUserId, String applicantName,
                             String applicantPhone, String applicantEmail,
                             String applicantPosition, String claimReason, String proofMaterials);

    /**
     * 取消自己的认领申请(仅PENDING状态)
     *
     * @param claimId 认领申请ID
     * @param userId  当前用户ID
     */
    void cancelClaim(Long claimId, Long userId);

    /**
     * 我的认领申请列表
     */
    PageResult<CompanyClaim> myClaims(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 管理员: 待审核认领申请列表
     */
    PageResult<CompanyClaim> pendingClaims(Integer pageNum, Integer pageSize);

    /**
     * 管理员: 按状态查询认领申请列表
     */
    PageResult<CompanyClaim> listByStatus(String status, Integer pageNum, Integer pageSize);

    /**
     * 管理员: 按企业查询认领申请列表
     */
    PageResult<CompanyClaim> listByCompany(Long companyId, Integer pageNum, Integer pageSize);

    /**
     * 管理员: 审核认领申请
     *
     * @param claimId       认领申请ID
     * @param reviewerUserId 审核人用户ID
     * @param reviewerName  审核人姓名
     * @param approved      true=通过 false=拒绝
     * @param reviewComment 审核意见
     * @return 更新后的认领申请
     */
    CompanyClaim reviewClaim(Long claimId, Long reviewerUserId, String reviewerName,
                             boolean approved, String reviewComment);

    /**
     * 获取企业的当前有效认领(企业详情页展示用)
     *
     * @param companyId 企业ID
     * @return 最近的认领记录, 无则返回null
     */
    CompanyClaim getLatestClaim(Long companyId);
}