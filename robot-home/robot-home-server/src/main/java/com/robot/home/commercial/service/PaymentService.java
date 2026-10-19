package com.robot.home.commercial.service;

import com.robot.home.commercial.entity.CommercialPayment;

import java.util.List;
import java.util.Map;

/**
 * 商业化支付服务
 * <p>
 * 安全设计：
 * 1. Mock支付仅沙箱环境可用，生产环境强制拒绝
 * 2. 真实支付渠道未配置时标记REAL_PAYMENT_NOT_CONFIGURED
 * 3. 回调验签+幂等处理
 */
public interface PaymentService {

    /**
     * 创建支付（为订单发起支付）
     *
     * @param orderId 订单ID
     * @param companyId 企业ID（隔离校验）
     * @param channel 支付渠道
     * @return 渠道侧支付参数
     */
    Map<String, Object> createPayment(Long orderId, Long companyId, String channel);

    /**
     * 处理支付回调（验签+幂等+确认订单）
     *
     * @param channel 渠道
     * @param params 回调参数
     * @param rawBody 原始报文
     * @return 处理结果
     */
    Map<String, Object> handleCallback(String channel, Map<String, String> params, String rawBody);

    /**
     * 查询订单支付记录
     */
    List<CommercialPayment> getPaymentsByOrderNo(String orderNo);

    /**
     * 列出可用支付渠道
     */
    List<Map<String, Object>> listAvailableChannels();

    /**
     * 沙箱模拟支付确认（仅沙箱环境，模拟用户完成支付）
     *
     * @param orderId 订单ID
     * @param companyId 企业ID
     * @return 模拟支付结果
     */
    Map<String, Object> sandboxConfirmPayment(Long orderId, Long companyId);
}