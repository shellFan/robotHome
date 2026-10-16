package com.robot.home.company.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.company.entity.CompanyClaim;
import com.robot.home.company.service.CompanyClaimService;
import com.robot.home.security.LoginUser;
import com.robot.home.security.RequirePermission;
import com.robot.home.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 后台企业认领审核 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/company-claims")
public class AdminCompanyClaimController {

    @Resource
    private CompanyClaimService companyClaimService;

    /**
     * 待审核认领申请列表
     */
    @GetMapping("/pending")
    @RequirePermission("company:claim:review")
    public Result<PageResult<CompanyClaim>> pending(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(companyClaimService.pendingClaims(pageNum, pageSize));
    }

    /**
     * 按状态查询认领申请列表
     */
    @GetMapping
    @RequirePermission("company:claim:review")
    public Result<PageResult<CompanyClaim>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(companyClaimService.listByStatus(status, pageNum, pageSize));
    }

    /**
     * 按企业查询认领申请列表
     */
    @GetMapping("/by-company")
    @RequirePermission("company:claim:review")
    public Result<PageResult<CompanyClaim>> byCompany(
            @RequestParam Long companyId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(companyClaimService.listByCompany(companyId, pageNum, pageSize));
    }

    /**
     * 审核认领申请
     */
    @PostMapping("/review")
    @RequirePermission("company:claim:review")
    public Result<CompanyClaim> review(
            @RequestParam Long claimId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reviewComment) {
        LoginUser admin = UserContext.getUser();
        Long reviewerId = admin != null ? admin.getUserId() : null;
        String reviewerName = admin != null ? admin.getUsername() : "admin";
        CompanyClaim claim = companyClaimService.reviewClaim(claimId, reviewerId,
                reviewerName, approved, reviewComment);
        return Result.success(claim);
    }
}