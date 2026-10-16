package com.robot.home.commercial.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.entity.EnterpriseEntitlementUsage;
import com.robot.home.commercial.mapper.EnterpriseEntitlementMapper;
import com.robot.home.commercial.mapper.EnterpriseEntitlementUsageMapper;
import com.robot.home.commercial.service.EntitlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业权益服务实现
 * <p>
 * 核心安全设计：
 * 1. 配额扣减使用CAS（WHERE quota_used < quota_total OR quota_total = 0）
 * 2. 月度重置幂等（检查period_start是否本月）
 * 3. 权益过期时间与订阅到期时间一致
 * 4. Redis故障不影响权益校验（直接查DB，fail-safe拒绝）
 */
@Slf4j
@Service
public class EntitlementServiceImpl implements EntitlementService {

    @Resource
    private EnterpriseEntitlementMapper entitlementMapper;
    @Resource
    private EnterpriseEntitlementUsageMapper usageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initEntitlements(Long companyId, Long subscriptionId, String entitlementConfig) {
        if (entitlementConfig == null || entitlementConfig.isEmpty()) {
            return;
        }
        List<JSONObject> configs;
        try {
            JSONArray arr = JSONUtil.parseArray(entitlementConfig);
            configs = arr.toList(JSONObject.class);
        } catch (Exception e) {
            log.warn("权益配置解析失败: {}", e.getMessage());
            return;
        }
        if (configs == null || configs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (JSONObject config : configs) {
            String code = config.getStr("code");
            if (code == null || code.isEmpty()) {
                continue;
            }
            // 已存在则跳过（幂等）
            EnterpriseEntitlement existing = entitlementMapper.selectOne(
                    Wrappers.<EnterpriseEntitlement>lambdaQuery()
                            .eq(EnterpriseEntitlement::getCompanyId, companyId)
                            .eq(EnterpriseEntitlement::getEntCode, code));
            if (existing != null) {
                continue;
            }
            int quotaTotal = toInt(config.get("quotaTotal"));
            String periodType = config.getStr("periodType");
            if (periodType == null || periodType.isEmpty()) {
                periodType = "MONTHLY";
            }

            EnterpriseEntitlement ent = new EnterpriseEntitlement();
            ent.setCompanyId(companyId);
            ent.setSubscriptionId(subscriptionId);
            ent.setEntCode(code);
            ent.setEntConfig(entitlementConfig);
            ent.setQuotaTotal(quotaTotal);
            ent.setQuotaUsed(0);
            ent.setPeriodType(periodType);
            ent.setStatus("ACTIVE");
            // 月度周期：本月1号到下月1号
            if ("MONTHLY".equals(periodType)) {
                ent.setPeriodStart(now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
                ent.setPeriodEnd(ent.getPeriodStart().plusMonths(1));
            }
            try {
                entitlementMapper.insert(ent);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发初始化，幂等跳过
                log.debug("权益并发初始化跳过: companyId={}, entCode={}", companyId, code);
            }
        }
    }

    @Override
    public EnterpriseEntitlement checkEntitlement(Long companyId, String entCode) {
        return entitlementMapper.selectOne(
                Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, companyId)
                        .eq(EnterpriseEntitlement::getEntCode, entCode)
                        .eq(EnterpriseEntitlement::getStatus, "ACTIVE"));
    }

    @Override
    public EnterpriseEntitlement requireEntitlement(Long companyId, String entCode) {
        EnterpriseEntitlement ent = checkEntitlement(companyId, entCode);
        if (ent == null) {
            throw new BusinessException("当前套餐不包含此权益，请升级套餐");
        }
        // 检查是否过期
        if (ent.getExpireTime() != null && ent.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("权益已过期，请续费");
        }
        // 月度权益检查周期是否需要重置
        if ("MONTHLY".equals(ent.getPeriodType()) && needReset(ent)) {
            resetMonthlyQuota(companyId);
            ent = checkEntitlement(companyId, entCode);
            if (ent == null) {
                throw new BusinessException("权益不可用");
            }
        }
        return ent;
    }

    @Override
    public boolean hasQuota(EnterpriseEntitlement entitlement) {
        if (entitlement == null) {
            return false;
        }
        // quotaTotal=0表示无限
        if (entitlement.getQuotaTotal() == 0) {
            return true;
        }
        return entitlement.getQuotaUsed() < entitlement.getQuotaTotal();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EnterpriseEntitlement consumeQuota(Long companyId, String entCode, String referenceType, Long referenceId) {
        EnterpriseEntitlement ent = requireEntitlement(companyId, entCode);
        if (!hasQuota(ent)) {
            throw new BusinessException("权益额度已用完，请升级套餐或等待下月重置");
        }
        // 无限额度不需要CAS扣减
        if (ent.getQuotaTotal() == 0) {
            recordUsage(companyId, ent.getId(), entCode, "CONSUME", referenceType, referenceId, 0);
            return ent;
        }
        // CAS扣减：WHERE quota_used < quota_total，防止并发超扣
        int updated = entitlementMapper.update(null,
                Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                        .setSql("quota_used = quota_used + 1")
                        .eq(EnterpriseEntitlement::getId, ent.getId())
                        .lt(EnterpriseEntitlement::getQuotaUsed, ent.getQuotaTotal()));
        if (updated == 0) {
            throw new BusinessException("权益额度已用完（并发扣减失败）");
        }
        recordUsage(companyId, ent.getId(), entCode, "CONSUME", referenceType, referenceId, 1);
        // 返回最新状态
        return entitlementMapper.selectById(ent.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseQuota(Long companyId, String entCode, String referenceType, Long referenceId) {
        EnterpriseEntitlement ent = checkEntitlement(companyId, entCode);
        if (ent == null || ent.getQuotaTotal() == 0) {
            return;
        }
        // 防止重复释放：检查是否已有对应RELEASE记录
        long released = usageMapper.selectCount(
                Wrappers.<EnterpriseEntitlementUsage>lambdaQuery()
                        .eq(EnterpriseEntitlementUsage::getEntitlementId, ent.getId())
                        .eq(EnterpriseEntitlementUsage::getUsageType, "RELEASE")
                        .eq(EnterpriseEntitlementUsage::getReferenceType, referenceType)
                        .eq(EnterpriseEntitlementUsage::getReferenceId, referenceId));
        if (released > 0) {
            return;
        }
        // CAS释放：quota_used > 0
        entitlementMapper.update(null,
                Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                        .setSql("quota_used = GREATEST(quota_used - 1, 0)")
                        .eq(EnterpriseEntitlement::getId, ent.getId())
                        .gt(EnterpriseEntitlement::getQuotaUsed, 0));
        recordUsage(companyId, ent.getId(), entCode, "RELEASE", referenceType, referenceId, -1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAllEntitlements(Long companyId) {
        entitlementMapper.update(null,
                Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                        .set(EnterpriseEntitlement::getStatus, "REVOKED")
                        .eq(EnterpriseEntitlement::getCompanyId, companyId)
                        .eq(EnterpriseEntitlement::getStatus, "ACTIVE"));
        log.info("撤销企业全部权益: companyId={}", companyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireEntitlements(Long companyId) {
        entitlementMapper.update(null,
                Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                        .set(EnterpriseEntitlement::getStatus, "EXPIRED")
                        .eq(EnterpriseEntitlement::getCompanyId, companyId)
                        .eq(EnterpriseEntitlement::getStatus, "ACTIVE")
                        .lt(EnterpriseEntitlement::getExpireTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetMonthlyQuota(Long companyId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        // 只重置周期在上月及之前的（幂等：本月已重置不再重复）
        List<EnterpriseEntitlement> needResets = entitlementMapper.selectList(
                Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, companyId)
                        .eq(EnterpriseEntitlement::getPeriodType, "MONTHLY")
                        .eq(EnterpriseEntitlement::getStatus, "ACTIVE")
                        .lt(EnterpriseEntitlement::getPeriodStart, monthStart));
        for (EnterpriseEntitlement ent : needResets) {
            entitlementMapper.update(null,
                    Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                            .set(EnterpriseEntitlement::getQuotaUsed, 0)
                            .set(EnterpriseEntitlement::getPeriodStart, monthStart)
                            .set(EnterpriseEntitlement::getPeriodEnd, monthEnd)
                            .eq(EnterpriseEntitlement::getId, ent.getId())
                            .lt(EnterpriseEntitlement::getPeriodStart, monthStart));
            recordUsage(companyId, ent.getId(), ent.getEntCode(), "RESET", null, null, 0);
        }
    }

    @Override
    public List<EnterpriseEntitlement> listEntitlements(Long companyId) {
        return entitlementMapper.selectList(
                Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, companyId)
                        .orderByAsc(EnterpriseEntitlement::getEntCode));
    }

    // ==================== 私有方法 ====================

    private boolean needReset(EnterpriseEntitlement ent) {
        if (ent.getPeriodEnd() == null) {
            return false;
        }
        return ent.getPeriodEnd().isBefore(LocalDateTime.now());
    }

    private void recordUsage(Long companyId, Long entitlementId, String entCode,
                             String usageType, String referenceType, Long referenceId, int delta) {
        EnterpriseEntitlementUsage usage = new EnterpriseEntitlementUsage();
        usage.setCompanyId(companyId);
        usage.setEntitlementId(entitlementId);
        usage.setEntCode(entCode);
        usage.setUsageType(usageType);
        usage.setReferenceType(referenceType);
        usage.setReferenceId(referenceId);
        usage.setDelta(delta);
        usageMapper.insert(usage);
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}