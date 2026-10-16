package com.robot.home.commercial.service;

import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.EnterpriseSubscription;

/**
 * 企业订阅服务
 * <p>
 * 订阅生命周期：FREE → ACTIVE → EXPIRING → EXPIRED → RENEWED
 */
public interface CommercialSubscriptionService {

    /**
     * 确保企业有FREE订阅（企业认领后调用）
     */
    void ensureFreeSubscription(Long companyId);

    /**
     * 从订单激活订阅（支付确认后调用，幂等）
     * 流程：订单PAID → 创建/更新订阅ACTIVE → 初始化权益
     */
    void activateFromOrder(CommercialOrder order);

    /**
     * 获取企业当前订阅
     */
    EnterpriseSubscription getSubscription(Long companyId);

    /**
     * 续费（延长到期时间）
     */
    void renew(Long companyId, Long orderId, int durationDays);

    /**
     * 取消订阅（标记，到期后失效）
     */
    void cancel(Long companyId);

    /**
     * 退款后权益调整（撤销权益+标记订阅）
     */
    void handleRefund(Long companyId, Long orderId);

    /**
     * 到期检查（定时任务）：ACTIVE→EXPIRING（7天内到期）→EXPIRED
     */
    int processExpirations();

    /**
     * Admin: 手动暂停/恢复订阅
     */
    void suspend(Long companyId, String reason);
    void resume(Long companyId);
}