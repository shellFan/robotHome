package com.robot.home.company.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.company.entity.Company;
import com.robot.home.company.entity.CompanyClaim;
import com.robot.home.company.mapper.CompanyClaimMapper;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.service.CompanyClaimService;
import com.robot.home.company.service.CompanyMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 企业认领服务实现
 */
@Slf4j
@Service
public class CompanyClaimServiceImpl implements CompanyClaimService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String VS_UNCLAIMED = "UNCLAIMED";
    private static final String VS_PENDING = "PENDING";
    private static final String VS_VERIFIED = "VERIFIED";
    private static final String VS_REJECTED = "REJECTED";

    @Resource
    private CompanyClaimMapper companyClaimMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private CompanyMemberService companyMemberService;

    @Override
    @Transactional
    public CompanyClaim submitClaim(Long companyId, Long applicantUserId, String applicantName,
                                    String applicantPhone, String applicantEmail,
                                    String applicantPosition, String claimReason, String proofMaterials) {
        // 1. 校验企业存在
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("企业不存在");
        }

        // 2. 校验企业未被认领
        if (VS_VERIFIED.equals(company.getVerificationStatus())) {
            throw new BusinessException("该企业已认证，无需重复认领");
        }

        // 3. 校验无待审核申请(uk_company_pending也在DB层保护)
        Long pendingCount = companyClaimMapper.selectCount(Wrappers.<CompanyClaim>lambdaQuery()
                .eq(CompanyClaim::getCompanyId, companyId)
                .eq(CompanyClaim::getStatus, STATUS_PENDING));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("该企业已有待审核的认领申请");
        }

        // 4. 校验用户无其他待审核申请(防止一人占多个企业)
        Long myPendingCount = companyClaimMapper.selectCount(Wrappers.<CompanyClaim>lambdaQuery()
                .eq(CompanyClaim::getApplicantUserId, applicantUserId)
                .eq(CompanyClaim::getStatus, STATUS_PENDING));
        if (myPendingCount != null && myPendingCount > 0) {
            throw new BusinessException("您已有待审核的认领申请，请等待审核完成");
        }

        // 5. 创建申请
        CompanyClaim claim = new CompanyClaim();
        claim.setCompanyId(companyId);
        claim.setApplicantUserId(applicantUserId);
        claim.setPendingCompanyId(companyId);
        claim.setPendingApplicantUserId(applicantUserId);
        claim.setApplicantName(applicantName);
        claim.setApplicantPhone(applicantPhone);
        claim.setApplicantEmail(applicantEmail);
        claim.setApplicantPosition(applicantPosition);
        claim.setClaimReason(claimReason);
        claim.setProofMaterials(proofMaterials);
        claim.setStatus(STATUS_PENDING);

        try {
            companyClaimMapper.insert(claim);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该企业已有待审核的认领申请");
        }

        // 6. 更新企业认证状态为PENDING
        Company update = new Company();
        update.setId(companyId);
        update.setVerificationStatus(VS_PENDING);
        companyMapper.updateById(update);

        log.info("企业认领申请提交: companyId={}, applicantUserId={}", companyId, applicantUserId);
        return claim;
    }

    @Override
    @Transactional
    public void cancelClaim(Long claimId, Long userId) {
        CompanyClaim claim = companyClaimMapper.selectById(claimId);
        if (claim == null) {
            throw new BusinessException("认领申请不存在");
        }
        if (!claim.getApplicantUserId().equals(userId)) {
            throw new BusinessException("只能取消自己的认领申请");
        }
        if (!STATUS_PENDING.equals(claim.getStatus())) {
            throw new BusinessException("只能取消待审核的认领申请");
        }

        int updated = companyClaimMapper.update(null, Wrappers.<CompanyClaim>lambdaUpdate()
                .eq(CompanyClaim::getId, claimId)
                .eq(CompanyClaim::getStatus, STATUS_PENDING)
                .set(CompanyClaim::getStatus, STATUS_CANCELLED)
                .set(CompanyClaim::getPendingCompanyId, null)
                .set(CompanyClaim::getPendingApplicantUserId, null)
                .set(CompanyClaim::getReviewerUserId, null)
                .set(CompanyClaim::getUpdateTime, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException("申请状态已变化，请刷新后重试");
        }

        // 恢复企业状态为UNCLAIMED(如果没有其他PENDING申请)
        restoreCompanyStatusIfNoPending(claim.getCompanyId());

        log.info("认领申请取消: claimId={}, userId={}", claimId, userId);
    }

    @Override
    public PageResult<CompanyClaim> myClaims(Long userId, Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
        Page<CompanyClaim> page = new Page<>(pn, ps);
        Page<CompanyClaim> result = companyClaimMapper.selectPage(page,
                Wrappers.<CompanyClaim>lambdaQuery()
                        .eq(CompanyClaim::getApplicantUserId, userId)
                        .orderByDesc(CompanyClaim::getCreateTime));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<CompanyClaim> pendingClaims(Integer pageNum, Integer pageSize) {
        return listByStatus(STATUS_PENDING, pageNum, pageSize);
    }

    @Override
    public PageResult<CompanyClaim> listByStatus(String status, Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
        Page<CompanyClaim> page = new Page<>(pn, ps);
        Page<CompanyClaim> result;
        if (status == null || status.isEmpty()) {
            result = companyClaimMapper.selectPage(page,
                    Wrappers.<CompanyClaim>lambdaQuery().orderByDesc(CompanyClaim::getCreateTime));
        } else {
            result = companyClaimMapper.selectPage(page,
                    Wrappers.<CompanyClaim>lambdaQuery()
                            .eq(CompanyClaim::getStatus, status)
                            .orderByDesc(CompanyClaim::getCreateTime));
        }
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<CompanyClaim> listByCompany(Long companyId, Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
        Page<CompanyClaim> page = new Page<>(pn, ps);
        Page<CompanyClaim> result = companyClaimMapper.selectPage(page,
                Wrappers.<CompanyClaim>lambdaQuery()
                        .eq(CompanyClaim::getCompanyId, companyId)
                        .orderByDesc(CompanyClaim::getCreateTime));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public CompanyClaim reviewClaim(Long claimId, Long reviewerUserId, String reviewerName,
                                    boolean approved, String reviewComment) {
        CompanyClaim claim = companyClaimMapper.selectById(claimId);
        if (claim == null) {
            throw new BusinessException("认领申请不存在");
        }
        if (!STATUS_PENDING.equals(claim.getStatus())) {
            throw new BusinessException("该申请已审核，不可重复操作");
        }

        String nextStatus = approved ? STATUS_APPROVED : STATUS_REJECTED;
        LocalDateTime reviewTime = LocalDateTime.now();
        int updated = companyClaimMapper.update(null, Wrappers.<CompanyClaim>lambdaUpdate()
                .eq(CompanyClaim::getId, claimId)
                .eq(CompanyClaim::getStatus, STATUS_PENDING)
                .set(CompanyClaim::getStatus, nextStatus)
                .set(CompanyClaim::getPendingCompanyId, null)
                .set(CompanyClaim::getPendingApplicantUserId, null)
                .set(CompanyClaim::getReviewerUserId, reviewerUserId)
                .set(CompanyClaim::getReviewerName, reviewerName)
                .set(CompanyClaim::getReviewTime, reviewTime)
                .set(CompanyClaim::getReviewComment, reviewComment)
                .set(CompanyClaim::getUpdateTime, reviewTime));
        if (updated != 1) {
            throw new BusinessException("申请状态已变化，请刷新后重试");
        }
        claim.setStatus(nextStatus);
        claim.setPendingCompanyId(null);
        claim.setPendingApplicantUserId(null);
        claim.setReviewerUserId(reviewerUserId);
        claim.setReviewerName(reviewerName);
        claim.setReviewTime(reviewTime);
        claim.setReviewComment(reviewComment);

        if (approved) {

            // 更新企业认证状态为VERIFIED
            int companyUpdated = companyMapper.update(null, Wrappers.<Company>lambdaUpdate()
                    .eq(Company::getId, claim.getCompanyId())
                    .eq(Company::getVerificationStatus, VS_PENDING)
                    .set(Company::getVerificationStatus, VS_VERIFIED));
            if (companyUpdated != 1) {
                throw new BusinessException("企业认证状态已变化，无法完成认领");
            }

            // 申请人成为企业OWNER成员
            companyMemberService.addMember(claim.getCompanyId(), claim.getApplicantUserId(), "OWNER");

            log.info("认领申请通过: claimId={}, companyId={}, applicantUserId={}",
                    claimId, claim.getCompanyId(), claim.getApplicantUserId());
        } else {
            // 企业状态恢复为UNCLAIMED
            restoreCompanyStatusIfNoPending(claim.getCompanyId());

            log.info("认领申请拒绝: claimId={}, companyId={}", claimId, claim.getCompanyId());
        }

        return claim;
    }

    @Override
    public CompanyClaim getLatestClaim(Long companyId) {
        return companyClaimMapper.selectOne(Wrappers.<CompanyClaim>lambdaQuery()
                .eq(CompanyClaim::getCompanyId, companyId)
                .orderByDesc(CompanyClaim::getCreateTime)
                .last("LIMIT 1"));
    }

    /**
     * 如果企业没有其他PENDING申请, 恢复verification_status为UNCLAIMED
     */
    private void restoreCompanyStatusIfNoPending(Long companyId) {
        Long pendingCount = companyClaimMapper.selectCount(Wrappers.<CompanyClaim>lambdaQuery()
                .eq(CompanyClaim::getCompanyId, companyId)
                .eq(CompanyClaim::getStatus, STATUS_PENDING));
        if (pendingCount == null || pendingCount == 0) {
            companyMapper.update(null, Wrappers.<Company>lambdaUpdate()
                    .eq(Company::getId, companyId)
                    .eq(Company::getVerificationStatus, VS_PENDING)
                    .set(Company::getVerificationStatus, VS_UNCLAIMED));
        }
    }
}
