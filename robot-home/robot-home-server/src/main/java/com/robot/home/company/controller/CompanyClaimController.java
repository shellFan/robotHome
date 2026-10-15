package com.robot.home.company.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.company.entity.CompanyClaim;
import com.robot.home.company.service.CompanyClaimService;
import com.robot.home.company.vo.CompanyClaimStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 企业认领 Controller (用户端)
 */
@Slf4j
@RestController
@RequestMapping("/api/company-claims")
public class CompanyClaimController {

    @Resource
    private CompanyClaimService companyClaimService;

    /**
     * 提交企业认领申请
     */
    @PostMapping("/submit")
    public Result<CompanyClaim> submit(
            @RequestParam Long companyId,
            @RequestParam String applicantName,
            @RequestParam String applicantPhone,
            @RequestParam(required = false) String applicantEmail,
            @RequestParam(required = false) String applicantPosition,
            @RequestParam String claimReason,
            @RequestParam(required = false) String proofMaterials) {
        Long userId = SecurityUtils.requireUserId();
        CompanyClaim claim = companyClaimService.submitClaim(companyId, userId,
                applicantName, applicantPhone, applicantEmail,
                applicantPosition, claimReason, proofMaterials);
        return Result.success(claim);
    }

    /**
     * 取消我的认领申请
     */
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestParam Long claimId) {
        Long userId = SecurityUtils.requireUserId();
        companyClaimService.cancelClaim(claimId, userId);
        return Result.success();
    }

    /**
     * 我的认领申请列表
     */
    @GetMapping("/my")
    public Result<PageResult<CompanyClaim>> myClaims(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(companyClaimService.myClaims(userId, pageNum, pageSize));
    }

    /**
     * 查询企业的当前认领状态(企业详情页展示)
     */
    @GetMapping("/latest")
    public Result<CompanyClaimStatusVO> latestClaim(@RequestParam Long companyId) {
        CompanyClaim claim = companyClaimService.getLatestClaim(companyId);
        if (claim == null) {
            return Result.success(null);
        }
        CompanyClaimStatusVO status = new CompanyClaimStatusVO();
        status.setCompanyId(claim.getCompanyId());
        status.setStatus(claim.getStatus());
        status.setCreateTime(claim.getCreateTime());
        status.setReviewTime(claim.getReviewTime());
        return Result.success(status);
    }
}
