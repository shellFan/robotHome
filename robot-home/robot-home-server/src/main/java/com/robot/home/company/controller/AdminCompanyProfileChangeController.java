package com.robot.home.company.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.company.entity.CompanyProfileChange;
import com.robot.home.company.service.CompanyProfileChangeService;
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
 * 后台企业资料变更审核 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/company-profile-changes")
public class AdminCompanyProfileChangeController {

    @Resource
    private CompanyProfileChangeService profileChangeService;

    /**
     * 按状态查询变更申请列表
     */
    @GetMapping
    @RequirePermission("company:profile:review")
    public Result<PageResult<CompanyProfileChange>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(profileChangeService.listByStatus(status, pageNum, pageSize));
    }

    /**
     * 审核变更申请
     */
    @PostMapping("/review")
    @RequirePermission("company:profile:review")
    public Result<CompanyProfileChange> review(
            @RequestParam Long changeId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reviewComment) {
        LoginUser admin = UserContext.getUser();
        Long reviewerId = admin != null ? admin.getUserId() : null;
        String reviewerName = admin != null ? admin.getUsername() : "admin";
        CompanyProfileChange change = profileChangeService.reviewChange(changeId, reviewerId,
                reviewerName, approved, reviewComment);
        return Result.success(change);
    }
}