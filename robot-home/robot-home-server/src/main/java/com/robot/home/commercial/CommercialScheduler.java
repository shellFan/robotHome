package com.robot.home.commercial;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.mapper.EnterpriseEntitlementMapper;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.EntitlementService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商业化定时任务
 * <p>
 * 1. 订单超时关闭（每5分钟）
 * 2. 订阅到期处理（每小时）
 * 3. 月度配额重置（每天凌晨3点）
 */
@Slf4j
@Component
@EnableScheduling
public class CommercialScheduler {

    @Resource
    private CommercialOrderService orderService;
    @Resource
    private CommercialSubscriptionService subscriptionService;
    @Resource
    private EntitlementService entitlementService;
    @Resource
    private EnterpriseEntitlementMapper entitlementMapper;
    @Resource
    private CompanyMapper companyMapper;

    /**
     * 订单超时关闭（每5分钟，30分钟未支付自动关闭）
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void closeExpiredOrders() {
        try {
            int closed = orderService.closeExpiredOrders(30);
            if (closed > 0) {
                log.info("[定时] 订单超时关闭: {}条", closed);
            }
        } catch (Exception e) {
            log.error("[定时] 订单超时关闭失败", e);
        }
    }

    /**
     * 订阅到期处理（每小时）
     * ACTIVE→EXPIRING（7天内到期）→EXPIRED（已过期）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void processSubscriptionExpirations() {
        try {
            int processed = subscriptionService.processExpirations();
            if (processed > 0) {
                log.info("[定时] 订阅到期处理: {}条", processed);
            }
        } catch (Exception e) {
            log.error("[定时] 订阅到期处理失败", e);
        }
    }

    /**
     * 月度配额重置（每天凌晨3点）
     * 检查所有有月度权益的企业，重置上月配额
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void resetMonthlyQuotas() {
        try {
            // 查所有有月度权益的企业
            List<EnterpriseEntitlement> monthlyEnts = entitlementMapper.selectList(
                    Wrappers.<EnterpriseEntitlement>lambdaQuery()
                            .eq(EnterpriseEntitlement::getPeriodType, "MONTHLY")
                            .eq(EnterpriseEntitlement::getStatus, "ACTIVE"));
            List<Long> companyIds = monthlyEnts.stream()
                    .map(EnterpriseEntitlement::getCompanyId)
                    .distinct()
                    .collect(Collectors.toList());
            int reset = 0;
            for (Long companyId : companyIds) {
                try {
                    entitlementService.resetMonthlyQuota(companyId);
                    reset++;
                } catch (Exception e) {
                    log.warn("[定时] 企业配额重置失败: companyId={}", companyId, e);
                }
            }
            log.info("[定时] 月度配额重置: companies={}/{}", reset, companyIds.size());
        } catch (Exception e) {
            log.error("[定时] 月度配额重置失败", e);
        }
    }
}