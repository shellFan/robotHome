package com.robot.home.commercial.service;

import com.robot.home.common.PageResult;
import com.robot.home.commercial.entity.CommercialOrder;

/**
 * 商业订单服务
 * <p>
 * 核心安全设计：
 * 1. 价格由服务端从套餐版本计算，不相信客户端提交金额
 * 2. 订单号唯一 + 幂等键唯一（双重防重）
 * 3. 状态机CAS更新，禁止非法跳转
 */
public interface CommercialOrderService {

    /**
     * 创建订单（幂等：同企业+同套餐版本+同幂等键返回已有订单）
     * 价格由服务端从套餐版本计算
     */
    CommercialOrder createOrder(Long companyId, Long userId, Long planId, String idempotentKey);

    /**
     * 查询订单（含企业归属校验）
     */
    CommercialOrder getOrder(Long orderId, Long companyId);

    /**
     * 按订单号查询
     */
    CommercialOrder getByOrderNo(String orderNo);

    /**
     * 企业订单列表
     */
    PageResult<CommercialOrder> listCompanyOrders(Long companyId, String status, Integer pageNum, Integer pageSize);

    /**
     * 取消订单（仅CREATED/PENDING_PAYMENT可取消）
     */
    void cancelOrder(Long orderId, Long companyId);

    /**
     * 支付确认（回调触发，CAS状态机 PENDING_PAYMENT→PAID）
     * 幂等：已PAID直接返回
     */
    void confirmPayment(Long orderId, String paymentChannel, String paymentNo);

    /**
     * 超时关闭（定时任务调用，CREATED/PENDING_PAYMENT超时→EXPIRED）
     */
    int closeExpiredOrders(int timeoutMinutes);

    /**
     * Admin: 全部订单列表
     */
    PageResult<CommercialOrder> adminListOrders(String status, Long companyId, Integer pageNum, Integer pageSize);

    /**
     * Admin: 订单详情（不校验企业归属）
     */
    CommercialOrder adminGetOrder(Long orderId);
}