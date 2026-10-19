package com.robot.home.commercial.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 商业订单接口（企业端）
 * <p>
 * 安全：所有接口通过CompanyMemberService校验企业成员身份
 */
@RestController
@RequestMapping("/api/commercial/orders")
public class CommercialOrderController {

    @Resource
    private CommercialOrderService orderService;
    @Resource
    private PaymentService paymentService;
    @Resource
    private CompanyMemberService companyMemberService;

    /**
     * 创建订单
     */
    @PostMapping
    public Result<CommercialOrder> createOrder(
            @RequestParam Long companyId,
            @RequestParam Long planId,
            @RequestParam String idempotentKey) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(orderService.createOrder(companyId, userId, planId, idempotentKey));
    }

    /**
     * 我的订单列表
     */
    @GetMapping
    public Result<PageResult<CommercialOrder>> listOrders(
            @RequestParam Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(orderService.listCompanyOrders(companyId, status, pageNum, pageSize));
    }

    /**
     * 订单详情
     */
    @GetMapping("/{orderId}")
    public Result<CommercialOrder> getOrder(
            @PathVariable Long orderId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(orderService.getOrder(orderId, companyId));
    }

    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        orderService.cancelOrder(orderId, companyId);
        return Result.success(null);
    }

    /**
     * 发起支付
     */
    @PostMapping("/{orderId}/pay")
    public Result<Map<String, Object>> createPayment(
            @PathVariable Long orderId,
            @RequestParam Long companyId,
            @RequestParam(defaultValue = "MOCK_SANDBOX") String channel) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(paymentService.createPayment(orderId, companyId, channel));
    }

    /**
     * 沙箱模拟支付确认（仅沙箱环境）
     */
    @PostMapping("/{orderId}/sandbox-confirm")
    public Result<Map<String, Object>> sandboxConfirm(
            @PathVariable Long orderId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(paymentService.sandboxConfirmPayment(orderId, companyId));
    }

    /**
     * 可用支付渠道
     */
    @GetMapping("/payment-channels")
    public Result<?> listPaymentChannels() {
        return Result.success(paymentService.listAvailableChannels());
    }
}