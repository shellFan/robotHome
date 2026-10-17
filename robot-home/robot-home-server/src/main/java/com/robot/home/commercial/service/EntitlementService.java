package com.robot.home.commercial.service;

import com.robot.home.commercial.entity.EnterpriseEntitlement;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业权益服务
 * <p>
 * 核心职责：权益检查、配额扣减/释放、月度重置
 * 所有商业权益必须在服务端校验，不能只在前端隐藏按钮
 */
public interface EntitlementService {

    /**
     * 初始化企业权益（订阅激活/套餐分配时调用）
     * @deprecated 使用带expireTime的重载方法，确保权益与订阅到期时间一致
     */
    @Deprecated
    void initEntitlements(Long companyId, Long subscriptionId, String entitlementConfig);

    /**
     * 初始化企业权益（指定权益到期时间，与订阅到期时间一致）
     * @param companyId 企业ID
     * @param subscriptionId 订阅ID
     * @param entitlementConfig 权益配置JSON
     * @param expireTime 权益到期时间（与订阅end_time一致，null则不设过期）
     */
    void initEntitlements(Long companyId, Long subscriptionId, String entitlementConfig, LocalDateTime expireTime);

    /**
     * 检查企业是否拥有某权益（不扣额度）
     * @return 权益实例，null表示无权益
     */
    EnterpriseEntitlement checkEntitlement(Long companyId, String entCode);

    /**
     * 检查并要求权益，无权益抛异常
     */
    EnterpriseEntitlement requireEntitlement(Long companyId, String entCode);

    /**
     * 检查配额是否充足
     */
    boolean hasQuota(EnterpriseEntitlement entitlement);

    /**
     * 消耗配额（CAS防并发超扣）
     * @param companyId 企业ID
     * @param entCode 权益编码
     * @param referenceType 关联类型
     * @param referenceId 关联ID
     * @return 消耗后的权益实例
     */
    EnterpriseEntitlement consumeQuota(Long companyId, String entCode, String referenceType, Long referenceId);

    /**
     * 释放配额（失败补偿）
     */
    void releaseQuota(Long companyId, String entCode, String referenceType, Long referenceId);

    /**
     * 撤销企业全部权益（退款/取消时）
     */
    void revokeAllEntitlements(Long companyId);

    /**
     * 失效过期权益
     */
    void expireEntitlements(Long companyId);

    /**
     * 月度配额重置（幂等）
     */
    void resetMonthlyQuota(Long companyId);

    /**
     * 查询企业全部权益
     */
    List<EnterpriseEntitlement> listEntitlements(Long companyId);
}