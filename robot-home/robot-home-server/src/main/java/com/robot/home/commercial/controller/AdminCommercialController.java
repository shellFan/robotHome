package com.robot.home.commercial.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.security.RequirePermission;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.CommercialRefund;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterprisePlanVersion;
import com.robot.home.commercial.entity.EnterpriseSubscription;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.PlanService;
import com.robot.home.commercial.service.RefundService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Admin商业化运营接口
 * <p>
 * 套餐管理+订单管理+退款管理+订阅管理
 */
@RestController
@RequestMapping("/api/admin/commercial")
public class AdminCommercialController {

    @Resource
    private PlanService planService;
    @Resource
    private CommercialOrderService orderService;
    @Resource
    private RefundService refundService;
    @Resource
    private CommercialSubscriptionService subscriptionService;

    // ==================== 套餐管理 ====================

    @GetMapping("/plans")
    @RequirePermission("commercial:plan:manage")
    public Result<PageResult<EnterprisePlan>> adminListPlans(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(planService.adminListPlans(status, pageNum, pageSize));
    }

    @PostMapping("/plans")
    @RequirePermission("commercial:plan:manage")
    public Result<EnterprisePlan> savePlan(@RequestBody EnterprisePlan plan) {
        return Result.success(planService.savePlan(plan));
    }

    @PostMapping("/plans/{planId}/status")
    @RequirePermission("commercial:plan:manage")
    public Result<Void> updatePlanStatus(
            @PathVariable Long planId,
            @RequestParam String status) {
        planService.updatePlanStatus(planId, status);
        return Result.success(null);
    }

    @PostMapping("/plans/{planId}/versions")
    @RequirePermission("commercial:plan:manage")
    public Result<EnterprisePlanVersion> createPlanVersion(
            @PathVariable Long planId,
            @RequestParam Long priceCents,
            @RequestParam Integer durationDays,
            @RequestParam String entitlementConfig) {
        return Result.success(planService.createPlanVersion(planId, priceCents, durationDays, entitlementConfig));
    }

    @GetMapping("/plans/{planId}/versions")
    @RequirePermission("commercial:plan:manage")
    public Result<List<EnterprisePlanVersion>> listVersions(@PathVariable Long planId) {
        return Result.success(planService.listVersions(planId));
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    @RequirePermission("commercial:order:manage")
    public Result<PageResult<CommercialOrder>> adminListOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(orderService.adminListOrders(status, companyId, pageNum, pageSize));
    }

    @GetMapping("/orders/{orderId}")
    @RequirePermission("commercial:order:manage")
    public Result<CommercialOrder> adminGetOrder(@PathVariable Long orderId) {
        return Result.success(orderService.adminGetOrder(orderId));
    }

    // ==================== 退款管理 ====================

    @PostMapping("/refunds")
    @RequirePermission("commercial:refund:manage")
    public Result<CommercialRefund> applyRefund(
            @RequestParam Long orderId,
            @RequestParam String reason) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(refundService.applyRefund(orderId, operatorId, reason));
    }

    @PostMapping("/refunds/{refundId}/confirm")
    @RequirePermission("commercial:refund:manage")
    public Result<Void> confirmRefund(@PathVariable Long refundId) {
        Long operatorId = SecurityUtils.requireUserId();
        refundService.confirmRefund(refundId, operatorId);
        return Result.success(null);
    }

    @PostMapping("/refunds/{refundId}/fail")
    @RequirePermission("commercial:refund:manage")
    public Result<Void> failRefund(
            @PathVariable Long refundId,
            @RequestParam String reason) {
        refundService.failRefund(refundId, reason);
        return Result.success(null);
    }

    @GetMapping("/refunds")
    @RequirePermission("commercial:refund:manage")
    public Result<PageResult<CommercialRefund>> adminListRefunds(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(refundService.adminListRefunds(status, pageNum, pageSize));
    }

    // ==================== 订阅管理 ====================

    @PostMapping("/subscriptions/{companyId}/suspend")
    @RequirePermission("commercial:subscription:manage")
    public Result<Void> suspendSubscription(
            @PathVariable Long companyId,
            @RequestParam String reason) {
        subscriptionService.suspend(companyId, reason);
        return Result.success(null);
    }

    @PostMapping("/subscriptions/{companyId}/resume")
    @RequirePermission("commercial:subscription:manage")
    public Result<Void> resumeSubscription(@PathVariable Long companyId) {
        subscriptionService.resume(companyId);
        return Result.success(null);
    }

    @GetMapping("/subscriptions/{companyId}")
    @RequirePermission("commercial:subscription:manage")
    public Result<EnterpriseSubscription> getSubscription(@PathVariable Long companyId) {
        return Result.success(subscriptionService.getSubscription(companyId));
    }
}