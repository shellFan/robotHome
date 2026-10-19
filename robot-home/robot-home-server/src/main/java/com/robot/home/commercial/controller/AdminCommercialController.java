package com.robot.home.commercial.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.security.RequirePermission;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.CommercialRefund;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterprisePlanVersion;
import com.robot.home.commercial.entity.EnterpriseSubscription;
import com.robot.home.commercial.mapper.CommercialOrderMapper;
import com.robot.home.commercial.mapper.CommercialRefundMapper;
import com.robot.home.commercial.mapper.EnterpriseSubscriptionMapper;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.PlanService;
import com.robot.home.commercial.service.RefundService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Resource
    private CommercialOrderMapper orderMapper;
    @Resource
    private CommercialRefundMapper refundMapper;
    @Resource
    private EnterpriseSubscriptionMapper subscriptionMapper;

    /** 沙箱/模拟渠道前缀列表（这些渠道的收入不计入真实收入） */
    private static final List<String> SANDBOX_CHANNELS = new ArrayList<>();
    static {
        SANDBOX_CHANNELS.add("MOCK_SANDBOX");
        SANDBOX_CHANNELS.add("MOCK");
        SANDBOX_CHANNELS.add("SANDBOX");
        SANDBOX_CHANNELS.add("DEMO");
    }

    // ==================== 商业统计看板 ====================

    /**
     * 商业统计看板
     * <p>
     * 安全设计：收入严格区分REAL与SANDBOX/MOCK/DEMO，
     * 沙箱渠道金额单独展示，绝不冒充真实收入。
     */
    @GetMapping("/stats")
    @RequirePermission("commercial:order:manage")
    public Result<Map<String, Object>> getCommercialStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 订单统计
        long totalOrders = orderMapper.selectCount(Wrappers.emptyWrapper());
        long pendingOrders = orderMapper.selectCount(
                Wrappers.<CommercialOrder>lambdaQuery().eq(CommercialOrder::getStatus, "PENDING_PAYMENT"));
        long paidOrders = orderMapper.selectCount(
                Wrappers.<CommercialOrder>lambdaQuery().eq(CommercialOrder::getStatus, "PAID"));
        long closedOrders = orderMapper.selectCount(
                Wrappers.<CommercialOrder>lambdaQuery().eq(CommercialOrder::getStatus, "CLOSED"));
        long refundedOrders = orderMapper.selectCount(
                Wrappers.<CommercialOrder>lambdaQuery().eq(CommercialOrder::getStatus, "REFUNDED"));
        Map<String, Object> orderStats = new HashMap<>();
        orderStats.put("total", totalOrders);
        orderStats.put("pendingPayment", pendingOrders);
        orderStats.put("paid", paidOrders);
        orderStats.put("closed", closedOrders);
        orderStats.put("refunded", refundedOrders);
        stats.put("orderStats", orderStats);

        // 2. 收入统计（严格区分REAL vs SANDBOX）
        List<CommercialOrder> paidOrderList = orderMapper.selectList(
                Wrappers.<CommercialOrder>lambdaQuery().eq(CommercialOrder::getStatus, "PAID"));
        long realRevenueCents = 0;
        long sandboxRevenueCents = 0;
        Map<String, Long> channelRevenue = new HashMap<>();
        for (CommercialOrder order : paidOrderList) {
            String channel = order.getPaymentChannel() != null ? order.getPaymentChannel() : "UNKNOWN";
            long amount = order.getPayableAmountCents() != null ? order.getPayableAmountCents() : 0L;
            channelRevenue.merge(channel, amount, Long::sum);
            if (SANDBOX_CHANNELS.contains(channel)) {
                sandboxRevenueCents += amount;
            } else {
                realRevenueCents += amount;
            }
        }
        Map<String, Object> revenueStats = new HashMap<>();
        revenueStats.put("realRevenueCents", realRevenueCents);
        revenueStats.put("sandboxRevenueCents", sandboxRevenueCents);
        revenueStats.put("totalRevenueCents", realRevenueCents + sandboxRevenueCents);
        revenueStats.put("channelRevenue", channelRevenue);
        revenueStats.put("realChannelCount", channelRevenue.keySet().stream()
                .filter(ch -> !SANDBOX_CHANNELS.contains(ch)).count());
        stats.put("revenueStats", revenueStats);

        // 3. 订阅统计
        long totalSubs = subscriptionMapper.selectCount(Wrappers.emptyWrapper());
        long activeSubs = subscriptionMapper.selectCount(
                Wrappers.<EnterpriseSubscription>lambdaQuery().eq(EnterpriseSubscription::getStatus, "ACTIVE"));
        long suspendedSubs = subscriptionMapper.selectCount(
                Wrappers.<EnterpriseSubscription>lambdaQuery().eq(EnterpriseSubscription::getStatus, "SUSPENDED"));
        long expiredSubs = subscriptionMapper.selectCount(
                Wrappers.<EnterpriseSubscription>lambdaQuery().eq(EnterpriseSubscription::getStatus, "EXPIRED"));
        Map<String, Object> subStats = new HashMap<>();
        subStats.put("total", totalSubs);
        subStats.put("active", activeSubs);
        subStats.put("suspended", suspendedSubs);
        subStats.put("expired", expiredSubs);
        stats.put("subscriptionStats", subStats);

        // 4. 退款统计
        long totalRefunds = refundMapper.selectCount(Wrappers.emptyWrapper());
        long applyingRefunds = refundMapper.selectCount(
                Wrappers.<CommercialRefund>lambdaQuery().eq(CommercialRefund::getStatus, "APPLYING"));
        long successRefunds = refundMapper.selectCount(
                Wrappers.<CommercialRefund>lambdaQuery().eq(CommercialRefund::getStatus, "SUCCESS"));
        long failedRefunds = refundMapper.selectCount(
                Wrappers.<CommercialRefund>lambdaQuery().eq(CommercialRefund::getStatus, "FAIL"));
        Map<String, Object> refundStats = new HashMap<>();
        refundStats.put("total", totalRefunds);
        refundStats.put("applying", applyingRefunds);
        refundStats.put("success", successRefunds);
        refundStats.put("failed", failedRefunds);
        stats.put("refundStats", refundStats);

        return Result.success(stats);
    }

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