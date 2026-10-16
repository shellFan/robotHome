package com.robot.home.commercial.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.robot.home.common.PageResult;
import com.robot.home.commercial.entity.CommercialRefund;

/**
 * 商业化退款服务
 * <p>
 * 退款流转：APPLYING → SUCCESS / FAIL
 * 退款成功后撤销权益（SubscriptionService.handleRefund）
 */
public interface RefundService {

    /**
     * 发起退款申请（Admin操作）
     *
     * @param orderId 订单ID
     * @param operatorId 操作人ID
     * @param reason 退款原因
     * @return 退款记录
     */
    CommercialRefund applyRefund(Long orderId, Long operatorId, String reason);

    /**
     * 确认退款成功（沙箱模式直接确认，真实模式需等待渠道回调）
     *
     * @param refundId 退款记录ID
     * @param operatorId 操作人ID
     */
    void confirmRefund(Long refundId, Long operatorId);

    /**
     * 标记退款失败
     *
     * @param refundId 退款记录ID
     * @param reason 失败原因
     */
    void failRefund(Long refundId, String reason);

    /**
     * 查询订单退款记录
     */
    java.util.List<CommercialRefund> getRefundsByOrderId(Long orderId);

    /**
     * Admin: 分页查询退款记录
     */
    PageResult<CommercialRefund> adminListRefunds(String status, Integer pageNum, Integer pageSize);
}