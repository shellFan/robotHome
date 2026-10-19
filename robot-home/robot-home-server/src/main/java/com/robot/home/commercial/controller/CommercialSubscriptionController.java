package com.robot.home.commercial.controller;

import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.entity.EnterpriseSubscription;
import com.robot.home.commercial.service.EntitlementService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 企业订阅与权益接口（企业端）
 */
@RestController
@RequestMapping("/api/commercial/subscription")
public class CommercialSubscriptionController {

    @Resource
    private CommercialSubscriptionService subscriptionService;
    @Resource
    private EntitlementService entitlementService;
    @Resource
    private CompanyMemberService companyMemberService;

    /**
     * 获取当前订阅
     */
    @GetMapping
    public Result<EnterpriseSubscription> getSubscription(@RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(subscriptionService.getSubscription(companyId));
    }

    /**
     * 获取企业权益列表
     */
    @GetMapping("/entitlements")
    public Result<List<EnterpriseEntitlement>> listEntitlements(@RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(entitlementService.listEntitlements(companyId));
    }

    /**
     * 取消自动续费
     */
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        subscriptionService.cancel(companyId);
        return Result.success(null);
    }
}