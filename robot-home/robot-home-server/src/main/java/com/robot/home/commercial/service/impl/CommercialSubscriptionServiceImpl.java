package com.robot.home.commercial.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.commercial.entity.CommercialAuditLog;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterprisePlanVersion;
import com.robot.home.commercial.entity.EnterpriseSubscription;
import com.robot.home.commercial.mapper.CommercialAuditLogMapper;
import com.robot.home.commercial.mapper.EnterpriseEntitlementMapper;
import com.robot.home.commercial.mapper.EnterprisePlanMapper;
import com.robot.home.commercial.mapper.EnterprisePlanVersionMapper;
import com.robot.home.commercial.mapper.EnterpriseSubscriptionMapper;
import com.robot.home.commercial.service.EntitlementService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import com.robot.home.company.entity.CompanyMember;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.message.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业订阅服务实现
 * <p>
 * 核心安全设计：
 * 1. activateFromOrder幂等：已ACTIVE且sourceOrderId相同则跳过
 * 2. uk_company_subscription保证一企业一订阅
 * 3. 到期检查CAS：EXPIRING→EXPIRED
 * 4. 退款后撤销权益
 */
@Slf4j
@Service
public class CommercialSubscriptionServiceImpl implements CommercialSubscriptionService {

    @Resource
    private EnterpriseSubscriptionMapper subscriptionMapper;
    @Resource
    private EnterprisePlanMapper planMapper;
    @Resource
    private EnterprisePlanVersionMapper planVersionMapper;
    @Resource
    private CommercialAuditLogMapper auditLogMapper;
    @Resource
    private EntitlementService entitlementService;
    @Resource
    private EnterpriseEntitlementMapper entitlementMapper;
    @Resource
    private CompanyMemberService companyMemberService;
    @Resource
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureFreeSubscription(Long companyId) {
        EnterpriseSubscription existing = getSubscription(companyId);
        if (existing != null) {
            return;
        }
        // 查FREE套餐
        EnterprisePlan freePlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery()
                        .eq(EnterprisePlan::getCode, "FREE"));
        if (freePlan == null) {
            log.warn("FREE套餐不存在，跳过初始化订阅: companyId={}", companyId);
            return;
        }
        EnterprisePlanVersion freeVersion = planVersionMapper.selectOne(
                Wrappers.<EnterprisePlanVersion>lambdaQuery()
                        .eq(EnterprisePlanVersion::getPlanId, freePlan.getId())
                        .eq(EnterprisePlanVersion::getStatus, "ACTIVE"));
        EnterpriseSubscription sub = new EnterpriseSubscription();
        sub.setCompanyId(companyId);
        sub.setPlanId(freePlan.getId());
        sub.setPlanVersionId(freeVersion != null ? freeVersion.getId() : 0L);
        sub.setPlanName(freePlan.getName());
        sub.setStatus("FREE");
        sub.setStartTime(LocalDateTime.now());
        sub.setEndTime(LocalDateTime.now().plusYears(100));
        sub.setAutoRenew(0);
        try {
            subscriptionMapper.insert(sub);
            // 初始化FREE权益（到期时间与订阅一致：100年）
            if (freeVersion != null) {
                entitlementService.initEntitlements(companyId, sub.getId(), freeVersion.getEntitlementConfig(), sub.getEndTime());
            }
        } catch (DuplicateKeyException e) {
            // 并发创建，幂等跳过
            log.debug("订阅并发创建跳过: companyId={}", companyId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateFromOrder(CommercialOrder order) {
        if (order == null || order.getCompanyId() == null) {
            return;
        }
        // 幂等：检查是否已由此订单激活
        EnterpriseSubscription existing = getSubscription(order.getCompanyId());
        if (existing != null && order.getId() != null && order.getId().equals(existing.getSourceOrderId())
                && "ACTIVE".equals(existing.getStatus())) {
            log.info("订阅已由此订单激活，幂等返回: companyId={}, orderId={}", order.getCompanyId(), order.getId());
            return;
        }
        // 解析套餐快照获取权益配置和有效期
        String snapshot = order.getPlanSnapshot();
        String entitlementConfig = "[]";
        int durationDays = 365;
        String planName = "";
        if (snapshot != null && !snapshot.isEmpty()) {
            try {
                JSONObject snap = JSONUtil.parseObj(snapshot);
                entitlementConfig = snap.getStr("entitlementConfig", "[]");
                durationDays = snap.getInt("durationDays", 365);
                planName = snap.getStr("planName", "");
            } catch (Exception e) {
                log.warn("套餐快照解析失败: orderId={}", order.getId());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusDays(durationDays);

        if (existing == null) {
            // 首次订阅
            EnterpriseSubscription sub = new EnterpriseSubscription();
            sub.setCompanyId(order.getCompanyId());
            sub.setPlanId(order.getPlanId());
            sub.setPlanVersionId(order.getPlanVersionId());
            sub.setPlanName(planName);
            sub.setStatus("ACTIVE");
            sub.setStartTime(now);
            sub.setEndTime(endTime);
            sub.setAutoRenew(0);
            sub.setSourceOrderId(order.getId());
            try {
                subscriptionMapper.insert(sub);
                entitlementService.initEntitlements(order.getCompanyId(), sub.getId(), entitlementConfig, endTime);
                updateEntitlementExpiry(order.getCompanyId(), endTime);
            } catch (DuplicateKeyException e) {
                log.debug("订阅并发激活跳过: companyId={}", order.getCompanyId());
            }
        } else {
            // 升级/续费：更新现有订阅
            // 先撤销旧权益
            entitlementService.revokeAllEntitlements(order.getCompanyId());
            // CAS更新订阅
            int updated = subscriptionMapper.update(null,
                    Wrappers.<EnterpriseSubscription>lambdaUpdate()
                            .set(EnterpriseSubscription::getPlanId, order.getPlanId())
                            .set(EnterpriseSubscription::getPlanVersionId, order.getPlanVersionId())
                            .set(EnterpriseSubscription::getPlanName, planName)
                            .set(EnterpriseSubscription::getStatus, "ACTIVE")
                            .set(EnterpriseSubscription::getStartTime, now)
                            .set(EnterpriseSubscription::getEndTime, endTime)
                            .set(EnterpriseSubscription::getSourceOrderId, order.getId())
                            .eq(EnterpriseSubscription::getId, existing.getId()));
            if (updated > 0) {
                entitlementService.initEntitlements(order.getCompanyId(), existing.getId(), entitlementConfig, endTime);
                updateEntitlementExpiry(order.getCompanyId(), endTime);
            }
        }
        auditLog("SYSTEM", null, order.getCompanyId(), "SUBSCRIPTION_ACTIVATE", "SUBSCRIPTION",
                existing != null ? existing.getId() : null, "orderId=" + order.getId());
        log.info("订阅激活: companyId={}, orderId={}, planId={}", order.getCompanyId(), order.getId(), order.getPlanId());
    }

    @Override
    public EnterpriseSubscription getSubscription(Long companyId) {
        return subscriptionMapper.selectOne(
                Wrappers.<EnterpriseSubscription>lambdaQuery()
                        .eq(EnterpriseSubscription::getCompanyId, companyId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renew(Long companyId, Long orderId, int durationDays) {
        EnterpriseSubscription sub = getSubscription(companyId);
        if (sub == null) {
            throw new BusinessException("订阅不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        // 续费：从当前到期时间或现在开始延长
        LocalDateTime base = sub.getEndTime() != null && sub.getEndTime().isAfter(now) ? sub.getEndTime() : now;
        LocalDateTime newEnd = base.plusDays(durationDays);
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getEndTime, newEnd)
                        .set(EnterpriseSubscription::getStatus, "ACTIVE")
                        .set(EnterpriseSubscription::getSourceOrderId, orderId)
                        .eq(EnterpriseSubscription::getId, sub.getId()));
        updateEntitlementExpiry(companyId, newEnd);
        auditLog("SYSTEM", null, companyId, "SUBSCRIPTION_RENEW", "SUBSCRIPTION", sub.getId(),
                "orderId=" + orderId + ",newEnd=" + newEnd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long companyId) {
        EnterpriseSubscription sub = getSubscription(companyId);
        if (sub == null) {
            throw new BusinessException("订阅不存在");
        }
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getAutoRenew, 0)
                        .set(EnterpriseSubscription::getCancelTime, LocalDateTime.now())
                        .eq(EnterpriseSubscription::getId, sub.getId()));
        auditLog("USER", null, companyId, "SUBSCRIPTION_CANCEL", "SUBSCRIPTION", sub.getId(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefund(Long companyId, Long orderId) {
        // 撤销全部权益
        entitlementService.revokeAllEntitlements(companyId);
        // 订阅降级为FREE
        EnterpriseSubscription sub = getSubscription(companyId);
        if (sub != null) {
            EnterprisePlan freePlan = planMapper.selectOne(
                    Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "FREE"));
            subscriptionMapper.update(null,
                    Wrappers.<EnterpriseSubscription>lambdaUpdate()
                            .set(EnterpriseSubscription::getStatus, "FREE")
                            .set(EnterpriseSubscription::getPlanId, freePlan != null ? freePlan.getId() : sub.getPlanId())
                            .set(EnterpriseSubscription::getPlanName, freePlan != null ? freePlan.getName() : sub.getPlanName())
                            .set(EnterpriseSubscription::getEndTime, LocalDateTime.now().plusYears(100))
                            .eq(EnterpriseSubscription::getId, sub.getId()));
            // 重新初始化FREE权益
            if (freePlan != null) {
                EnterprisePlanVersion freeVersion = planVersionMapper.selectOne(
                        Wrappers.<EnterprisePlanVersion>lambdaQuery()
                                .eq(EnterprisePlanVersion::getPlanId, freePlan.getId())
                                .eq(EnterprisePlanVersion::getStatus, "ACTIVE"));
                if (freeVersion != null) {
                    entitlementService.initEntitlements(companyId, sub.getId(), freeVersion.getEntitlementConfig(), LocalDateTime.now().plusYears(100));
                }
            }
        }
        auditLog("SYSTEM", null, companyId, "SUBSCRIPTION_REFUND", "SUBSCRIPTION",
                sub != null ? sub.getId() : null, "orderId=" + orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processExpirations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusDays(7);
        int count = 0;
        // ACTIVE→EXPIRING（7天内到期）
        List<EnterpriseSubscription> expiringSoon = subscriptionMapper.selectList(
                Wrappers.<EnterpriseSubscription>lambdaQuery()
                        .eq(EnterpriseSubscription::getStatus, "ACTIVE")
                        .le(EnterpriseSubscription::getEndTime, soon)
                        .gt(EnterpriseSubscription::getEndTime, now));
        for (EnterpriseSubscription sub : expiringSoon) {
            subscriptionMapper.update(null,
                    Wrappers.<EnterpriseSubscription>lambdaUpdate()
                            .set(EnterpriseSubscription::getStatus, "EXPIRING")
                            .eq(EnterpriseSubscription::getId, sub.getId())
                            .eq(EnterpriseSubscription::getStatus, "ACTIVE"));
            count++;
            // 通知企业管理员订阅即将到期
            notifyCompanyAdmins(sub.getCompanyId(), "订阅即将到期",
                    "您的订阅\"" + sub.getPlanName() + "\"将于" + sub.getEndTime().toLocalDate() + "到期，请及时续费",
                    "SUBSCRIPTION_EXPIRING:" + sub.getId());
        }
        // EXPIRING/ACTIVE→EXPIRED（已过期）
        List<EnterpriseSubscription> expired = subscriptionMapper.selectList(
                Wrappers.<EnterpriseSubscription>lambdaQuery()
                        .in(EnterpriseSubscription::getStatus, "ACTIVE", "EXPIRING")
                        .le(EnterpriseSubscription::getEndTime, now));
        for (EnterpriseSubscription sub : expired) {
            int updated = subscriptionMapper.update(null,
                    Wrappers.<EnterpriseSubscription>lambdaUpdate()
                            .set(EnterpriseSubscription::getStatus, "EXPIRED")
                            .eq(EnterpriseSubscription::getId, sub.getId())
                            .in(EnterpriseSubscription::getStatus, "ACTIVE", "EXPIRING"));
            if (updated > 0) {
                // 失效权益
                entitlementService.expireEntitlements(sub.getCompanyId());
                count++;
            }
        }
        if (count > 0) {
            log.info("订阅到期处理: {}条", count);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspend(Long companyId, String reason) {
        EnterpriseSubscription sub = getSubscription(companyId);
        if (sub == null) {
            throw new BusinessException("订阅不存在");
        }
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getStatus, "SUSPENDED")
                        .set(EnterpriseSubscription::getSuspendReason, reason)
                        .eq(EnterpriseSubscription::getId, sub.getId()));
        entitlementService.revokeAllEntitlements(companyId);
        auditLog("ADMIN", null, companyId, "SUBSCRIPTION_SUSPEND", "SUBSCRIPTION", sub.getId(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resume(Long companyId) {
        EnterpriseSubscription sub = getSubscription(companyId);
        if (sub == null) {
            throw new BusinessException("订阅不存在");
        }
        if (!"SUSPENDED".equals(sub.getStatus())) {
            throw new BusinessException("仅暂停状态可恢复");
        }
        // 恢复为ACTIVE（如果未过期）
        String newStatus = sub.getEndTime() != null && sub.getEndTime().isAfter(LocalDateTime.now()) ? "ACTIVE" : "EXPIRED";
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getStatus, newStatus)
                        .set(EnterpriseSubscription::getSuspendReason, null)
                        .eq(EnterpriseSubscription::getId, sub.getId()));
        auditLog("ADMIN", null, companyId, "SUBSCRIPTION_RESUME", "SUBSCRIPTION", sub.getId(), null);
    }

    // ==================== 私有方法 ====================

    /**
     * 通知企业管理员（OWNER角色成员）
     */
    private void notifyCompanyAdmins(Long companyId, String title, String summary, String eventKey) {
        try {
            List<CompanyMember> members = companyMemberService.listMembers(companyId);
            for (CompanyMember member : members) {
                if ("OWNER".equals(member.getRole()) || "ADMIN".equals(member.getRole())) {
                    notificationService.sendNotification(member.getUserId(), "SYSTEM",
                            title, summary, "company", companyId, eventKey);
                }
            }
        } catch (Exception e) {
            log.warn("企业管理员通知发送失败: companyId={}", companyId, e);
        }
    }

    private void updateEntitlementExpiry(Long companyId, LocalDateTime endTime) {
        // 权益过期时间与订阅到期时间一致（实际更新DB，确保订阅过期后权益自动失效）
        List<EnterpriseEntitlement> ents = entitlementService.listEntitlements(companyId);
        for (EnterpriseEntitlement ent : ents) {
            if ("ACTIVE".equals(ent.getStatus())) {
                entitlementMapper.update(null,
                        Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                                .set(EnterpriseEntitlement::getExpireTime, endTime)
                                .eq(EnterpriseEntitlement::getId, ent.getId()));
            }
        }
    }

    private void auditLog(String operatorType, Long operatorId, Long companyId,
                          String action, String entityType, Long entityId, String detail) {
        try {
            CommercialAuditLog audit = new CommercialAuditLog();
            audit.setOperatorType(operatorType);
            audit.setOperatorId(operatorId);
            audit.setCompanyId(companyId);
            audit.setAction(action);
            audit.setEntityType(entityType);
            audit.setEntityId(entityId);
            audit.setDetail(detail);
            auditLogMapper.insert(audit);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }
}