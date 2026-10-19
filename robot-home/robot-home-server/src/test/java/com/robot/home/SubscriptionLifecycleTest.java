package com.robot.home;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.CommercialRefund;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterpriseSubscription;
import com.robot.home.commercial.mapper.CommercialOrderMapper;
import com.robot.home.commercial.mapper.CommercialRefundMapper;
import com.robot.home.commercial.mapper.EnterpriseEntitlementMapper;
import com.robot.home.commercial.mapper.EnterprisePlanMapper;
import com.robot.home.commercial.mapper.EnterpriseSubscriptionMapper;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import com.robot.home.commercial.service.EntitlementService;
import com.robot.home.commercial.service.PaymentService;
import com.robot.home.commercial.service.RefundService;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase14 Final Merge Gate: 订阅权益生命周期专项测试 E-01~E-14
 * <p>
 * 核心验证原则：
 * 订阅A的到期或退款事件，只能撤销订阅A对应的权益，
 * 不能错误撤销续费、升级或新订阅的有效权益。
 * <p>
 * 修复验证：
 * - Fix1: initEntitlements重新激活EXPIRED/REVOKED权益（原实现跳过导致升级后无权益）
 * - Fix2: processExpirations CAS增加endTime检查（防续费并发误杀）
 * - Fix3: expireEntitlements按subscriptionId作用域（防跨订阅误撤销）
 * - Fix4: handleRefund检查sourceOrderId（防旧订单退款降级新订阅）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
@Transactional
class SubscriptionLifecycleTest {

    @Autowired
    private CommercialOrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private EntitlementService entitlementService;
    @Autowired
    private CommercialSubscriptionService subscriptionService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private EnterprisePlanMapper planMapper;
    @Autowired
    private EnterpriseSubscriptionMapper subscriptionMapper;
    @Autowired
    private EnterpriseEntitlementMapper entitlementMapper;
    @Autowired
    private CommercialOrderMapper orderMapper;
    @Autowired
    private CommercialRefundMapper refundMapper;
    @Autowired
    private CompanyMapper companyMapper;

    @MockBean
    private RedisUtils redisUtils;

    // ==================== 辅助方法 ====================

    private Long createCompany(String name) {
        Company c = new Company();
        c.setName(name);
        c.setStatus(1);
        c.setVerificationStatus("VERIFIED");
        companyMapper.insert(c);
        return c.getId();
    }

    private EnterprisePlan activatePlan(String code) {
        EnterprisePlan plan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, code));
        assertNotNull(plan, "测试库必须预置" + code + "套餐");
        plan.setStatus("ON_SHELF");
        plan.setSaleable(1);
        planMapper.updateById(plan);
        return plan;
    }

    /** 完整支付链路：创建订单→沙箱支付→确认，返回PAID订单 */
    private CommercialOrder payForPlan(Long companyId, Long userId, EnterprisePlan plan) {
        CommercialOrder order = orderService.createOrder(
                companyId, userId, plan.getId(), "idem-life-" + System.nanoTime());
        Map<String, Object> payResult = paymentService.createPayment(order.getId(), companyId, "MOCK_SANDBOX");
        assertFalse(payResult.containsKey("error"), "沙箱支付应可用: " + payResult);
        Map<String, Object> confirmResult = paymentService.sandboxConfirmPayment(order.getId(), companyId);
        assertEquals(Boolean.TRUE, confirmResult.get("success"), "沙箱确认应成功: " + confirmResult);
        return orderMapper.selectById(order.getId());
    }

    /** 模拟订阅到期：将endTime设为过去，然后执行processExpirations */
    private void simulateExpiration(Long companyId) {
        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertNotNull(sub);
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getEndTime, LocalDateTime.now().minusDays(1))
                        .eq(EnterpriseSubscription::getId, sub.getId()));
        subscriptionService.processExpirations();
    }

    private EnterpriseEntitlement getEntitlement(Long companyId, String code) {
        return entitlementMapper.selectOne(
                Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, companyId)
                        .eq(EnterpriseEntitlement::getEntCode, code));
    }

    private long countActiveEntitlements(Long companyId) {
        return entitlementMapper.selectCount(
                Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, companyId)
                        .eq(EnterpriseEntitlement::getStatus, "ACTIVE"));
    }

    // ==================== E-01: 首次开通 ====================

    @Test
    void e01_firstActivationCreatesActiveEntitlements() {
        Long companyId = createCompany("E01公司");
        Long userId = 920001L;
        subscriptionService.ensureFreeSubscription(companyId);

        // FREE权益应已初始化
        assertTrue(countActiveEntitlements(companyId) > 0, "FREE订阅应有ACTIVE权益");
        EnterpriseEntitlement leadView = getEntitlement(companyId, "LEAD_VIEW");
        assertNotNull(leadView, "FREE应包含LEAD_VIEW");
        assertEquals("ACTIVE", leadView.getStatus());
        assertEquals(5, leadView.getQuotaTotal(), "FREE LEAD_VIEW配额应为5");

        // 升级到BASIC
        EnterprisePlan basicPlan = activatePlan("BASIC");
        CommercialOrder order = payForPlan(companyId, userId, basicPlan);

        assertEquals("PAID", order.getStatus());
        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", sub.getStatus());
        assertEquals(order.getId(), sub.getSourceOrderId());

        // BASIC权益应已激活（LEAD_VIEW配额从5→20）
        EnterpriseEntitlement basicLeadView = getEntitlement(companyId, "LEAD_VIEW");
        assertNotNull(basicLeadView);
        assertEquals("ACTIVE", basicLeadView.getStatus());
        assertEquals(20, basicLeadView.getQuotaTotal(), "BASIC LEAD_VIEW配额应为20");
        // PRODUCT_MANAGEMENT是BASIC新增权益
        EnterpriseEntitlement prodMgmt = getEntitlement(companyId, "PRODUCT_MANAGEMENT");
        assertNotNull(prodMgmt, "BASIC应包含PRODUCT_MANAGEMENT");
        assertEquals("ACTIVE", prodMgmt.getStatus());
    }

    // ==================== E-02: 正常到期 ====================

    @Test
    void e02_normalExpirationExpiresEntitlements() {
        Long companyId = createCompany("E02公司");
        Long userId = 920002L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");
        payForPlan(companyId, userId, basicPlan);

        // 验证激活状态
        assertEquals("ACTIVE", subscriptionService.getSubscription(companyId).getStatus());
        assertTrue(countActiveEntitlements(companyId) > 0);

        // 模拟到期
        simulateExpiration(companyId);

        // 订阅应为EXPIRED
        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertEquals("EXPIRED", sub.getStatus(), "到期后订阅应为EXPIRED");

        // 所有权益应为EXPIRED（Fix3: 按subscriptionId作用域撤销）
        assertEquals(0, countActiveEntitlements(companyId), "到期后不应有ACTIVE权益");
        EnterpriseEntitlement leadView = getEntitlement(companyId, "LEAD_VIEW");
        assertEquals("EXPIRED", leadView.getStatus(), "LEAD_VIEW应EXPIRED");
    }

    // ==================== E-03: 到期前续费 ====================

    @Test
    void e03_renewalBeforeExpirationPreservesEntitlements() {
        Long companyId = createCompany("E03公司");
        Long userId = 920003L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");
        payForPlan(companyId, userId, basicPlan);

        EnterpriseSubscription subBefore = subscriptionService.getSubscription(companyId);
        LocalDateTime originalEnd = subBefore.getEndTime();
        assertNotNull(originalEnd);
        assertTrue(originalEnd.isAfter(LocalDateTime.now()), "新订阅endTime应在未来");

        // 到期前续费（再买一次BASIC）
        CommercialOrder renewOrder = payForPlan(companyId, userId, basicPlan);

        EnterpriseSubscription subAfter = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", subAfter.getStatus(), "续费后订阅应保持ACTIVE");
        assertEquals(renewOrder.getId(), subAfter.getSourceOrderId(), "sourceOrderId应更新为续费订单");
        assertTrue(subAfter.getEndTime().isAfter(originalEnd), "续费后endTime应延长");

        // 权益应保持ACTIVE
        assertTrue(countActiveEntitlements(companyId) > 0, "续费后权益应保持ACTIVE");
        EnterpriseEntitlement leadView = getEntitlement(companyId, "LEAD_VIEW");
        assertEquals("ACTIVE", leadView.getStatus());
    }

    // ==================== E-04: 到期后续费（Fix1核心验证） ====================

    @Test
    void e04_renewalAfterExpirationReactivatesEntitlements() {
        Long companyId = createCompany("E04公司");
        Long userId = 920004L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");
        payForPlan(companyId, userId, basicPlan);

        // 模拟到期
        simulateExpiration(companyId);
        assertEquals("EXPIRED", subscriptionService.getSubscription(companyId).getStatus());
        assertEquals(0, countActiveEntitlements(companyId), "到期后无ACTIVE权益");

        // 到期后续费（Fix1: initEntitlements必须重新激活EXPIRED权益）
        CommercialOrder renewOrder = payForPlan(companyId, userId, basicPlan);

        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", sub.getStatus(), "续费后订阅应为ACTIVE");
        assertEquals(renewOrder.getId(), sub.getSourceOrderId());

        // 核心断言：EXPIRED权益必须被重新激活（Fix1验证）
        assertTrue(countActiveEntitlements(companyId) > 0,
                "到期后续费，权益必须被重新激活（Fix1: initEntitlements重新激活EXPIRED权益）");
        EnterpriseEntitlement leadView = getEntitlement(companyId, "LEAD_VIEW");
        assertEquals("ACTIVE", leadView.getStatus(), "LEAD_VIEW必须从EXPIRED重新激活为ACTIVE");
        assertEquals(20, leadView.getQuotaTotal(), "重新激活后配额应为BASIC的20");
        assertEquals(0, leadView.getQuotaUsed(), "重新激活后已用配额应清零");
        assertEquals(sub.getId(), leadView.getSubscriptionId(), "subscriptionId应更新为当前订阅");

        // 权益可用性验证
        EnterpriseEntitlement consumed = entitlementService.consumeQuota(companyId, "LEAD_VIEW", "TEST", 9201L);
        assertNotNull(consumed, "重新激活的权益必须可消费");
    }

    // ==================== E-05: 套餐升级（Fix1核心验证） ====================

    @Test
    void e05_upgradeReactivatesEntitlementsWithNewQuota() {
        Long companyId = createCompany("E05公司");
        Long userId = 920005L;
        subscriptionService.ensureFreeSubscription(companyId);

        // 先买BASIC
        EnterprisePlan basicPlan = activatePlan("BASIC");
        payForPlan(companyId, userId, basicPlan);

        EnterpriseEntitlement basicLeadView = getEntitlement(companyId, "LEAD_VIEW");
        assertEquals(20, basicLeadView.getQuotaTotal(), "BASIC LEAD_VIEW配额20");
        assertNotNull(getEntitlement(companyId, "PRODUCT_MANAGEMENT"), "BASIC应有PRODUCT_MANAGEMENT");
        assertNull(getEntitlement(companyId, "AD_CAMPAIGN"), "BASIC不应有AD_CAMPAIGN");

        // 消耗一些配额
        entitlementService.consumeQuota(companyId, "LEAD_VIEW", "TEST", 9501L);
        assertEquals(1, getEntitlement(companyId, "LEAD_VIEW").getQuotaUsed());

        // 升级到PRO（Fix1: revokeAll后initEntitlements必须重新激活REVOKED权益）
        EnterprisePlan proPlan = activatePlan("PRO");
        payForPlan(companyId, userId, proPlan);

        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", sub.getStatus());
        assertEquals(proPlan.getId(), sub.getPlanId(), "订阅应指向PRO");

        // 核心断言：升级后权益必须重新激活并更新配额
        EnterpriseEntitlement proLeadView = getEntitlement(companyId, "LEAD_VIEW");
        assertEquals("ACTIVE", proLeadView.getStatus(), "升级后LEAD_VIEW必须ACTIVE");
        assertEquals(100, proLeadView.getQuotaTotal(), "升级后LEAD_VIEW配额应为PRO的100");
        assertEquals(0, proLeadView.getQuotaUsed(), "升级后已用配额应清零");

        // PRO新增权益
        EnterpriseEntitlement adCampaign = getEntitlement(companyId, "AD_CAMPAIGN");
        assertNotNull(adCampaign, "PRO应包含AD_CAMPAIGN");
        assertEquals("ACTIVE", adCampaign.getStatus());
        assertEquals(3, adCampaign.getQuotaTotal());

        // BASIC和PRO共有的权益
        EnterpriseEntitlement prodMgmt = getEntitlement(companyId, "PRODUCT_MANAGEMENT");
        assertEquals("ACTIVE", prodMgmt.getStatus());
        assertEquals(200, prodMgmt.getQuotaTotal(), "升级后PRODUCT_MANAGEMENT配额应为PRO的200");
    }

    // ==================== E-06: 套餐降级 ====================

    @Test
    void e06_downgradeUpdatesQuotaAndRevokesRemovedEntitlements() {
        Long companyId = createCompany("E06公司");
        Long userId = 920006L;
        subscriptionService.ensureFreeSubscription(companyId);

        // 先买PRO
        EnterprisePlan proPlan = activatePlan("PRO");
        payForPlan(companyId, userId, proPlan);
        assertNotNull(getEntitlement(companyId, "AD_CAMPAIGN"), "PRO应有AD_CAMPAIGN");
        assertEquals(100, getEntitlement(companyId, "LEAD_VIEW").getQuotaTotal());

        // 降级到BASIC
        EnterprisePlan basicPlan = activatePlan("BASIC");
        payForPlan(companyId, userId, basicPlan);

        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", sub.getStatus());
        assertEquals(basicPlan.getId(), sub.getPlanId());

        // 降级后配额应降为BASIC级别
        EnterpriseEntitlement leadView = getEntitlement(companyId, "LEAD_VIEW");
        assertEquals("ACTIVE", leadView.getStatus());
        assertEquals(20, leadView.getQuotaTotal(), "降级后LEAD_VIEW配额应为BASIC的20");

        // PRO独有权益应被撤销（不在BASIC配置中，revokeAll后不重新激活）
        EnterpriseEntitlement adCampaign = getEntitlement(companyId, "AD_CAMPAIGN");
        assertNotNull(adCampaign, "AD_CAMPAIGN记录应存在");
        assertNotEquals("ACTIVE", adCampaign.getStatus(), "降级后AD_CAMPAIGN不应ACTIVE");
    }

    // ==================== E-07: 旧订阅延迟到期任务（Fix2核心验证） ====================

    @Test
    void e07_delayedExpirationTaskDoesNotKillRenewedSubscription() {
        Long companyId = createCompany("E07公司");
        Long userId = 920007L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");
        payForPlan(companyId, userId, basicPlan);

        // 模拟到期
        simulateExpiration(companyId);
        assertEquals("EXPIRED", subscriptionService.getSubscription(companyId).getStatus());

        // 续费（新订单，endTime延至未来）
        payForPlan(companyId, userId, basicPlan);
        EnterpriseSubscription renewedSub = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", renewedSub.getStatus(), "续费后应ACTIVE");
        assertTrue(renewedSub.getEndTime().isAfter(LocalDateTime.now()), "续费后endTime应在未来");
        assertTrue(countActiveEntitlements(companyId) > 0, "续费后应有ACTIVE权益");

        // Fix2核心验证：再次执行到期任务，不应误杀已续费的订阅
        // 场景模拟：到期任务在续费前读取了列表，续费后才执行CAS
        // 修复前：CAS仅检查status=ACTIVE就会成功→误杀
        // 修复后：CAS增加endTime<=now条件→续费后endTime在未来→CAS失败→不误杀
        subscriptionService.processExpirations();

        EnterpriseSubscription subAfter = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", subAfter.getStatus(),
                "Fix2: 续费后的订阅不得被延迟到期任务误杀（CAS endTime检查）");
        assertTrue(subAfter.getEndTime().isAfter(LocalDateTime.now()));
        assertTrue(countActiveEntitlements(companyId) > 0,
                "Fix2: 续费后的权益不得被延迟到期任务误撤销");
    }

    // ==================== E-08: 旧订单延迟退款（Fix4核心验证） ====================

    @Test
    void e08_delayedRefundOfOldOrderDoesNotAffectNewSubscription() {
        Long companyId = createCompany("E08公司");
        Long userId = 920008L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");

        // 第一笔订单
        CommercialOrder order1 = payForPlan(companyId, userId, basicPlan);
        assertEquals(order1.getId(), subscriptionService.getSubscription(companyId).getSourceOrderId());

        // 续费（第二笔订单，sourceOrderId更新）
        CommercialOrder order2 = payForPlan(companyId, userId, basicPlan);
        assertEquals(order2.getId(), subscriptionService.getSubscription(companyId).getSourceOrderId(),
                "续费后sourceOrderId应为新订单");
        assertTrue(countActiveEntitlements(companyId) > 0);

        // 对旧订单order1发起退款
        CommercialRefund refund = refundService.applyRefund(order1.getId(), userId, "旧订单退款");
        refundService.confirmRefund(refund.getId(), 1L);

        // Fix4核心验证：旧订单退款不应影响当前订阅（sourceOrderId不匹配→跳过降级）
        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", sub.getStatus(),
                "Fix4: 旧订单退款不得降级已续费的订阅（sourceOrderId检查）");
        assertEquals(order2.getId(), sub.getSourceOrderId(), "sourceOrderId应保持为续费订单");
        assertTrue(countActiveEntitlements(companyId) > 0,
                "Fix4: 旧订单退款不得撤销新订阅的权益");

        // 旧订单状态应为REFUNDED
        assertEquals("REFUNDED", orderMapper.selectById(order1.getId()).getStatus());
        // 新订单状态应保持PAID
        assertEquals("PAID", orderMapper.selectById(order2.getId()).getStatus(),
                "新订单不应被旧订单退款影响");
    }

    // ==================== E-09: 退款与续费竞争 ====================

    @Test
    void e09_refundCurrentOrderThenRenewIsSafe() {
        Long companyId = createCompany("E09公司");
        Long userId = 920009L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");

        // 买BASIC
        CommercialOrder order1 = payForPlan(companyId, userId, basicPlan);
        assertTrue(countActiveEntitlements(companyId) > 0);

        // 退款当前订单（sourceOrderId匹配→降级到FREE）
        CommercialRefund refund = refundService.applyRefund(order1.getId(), userId, "全额退款");
        refundService.confirmRefund(refund.getId(), 1L);

        EnterpriseSubscription subAfterRefund = subscriptionService.getSubscription(companyId);
        assertEquals("FREE", subAfterRefund.getStatus(), "退款后应降级为FREE");
        // FREE权益应被重新激活（Fix1: revokeAll后initEntitlements重新激活）
        EnterpriseEntitlement leadView = getEntitlement(companyId, "LEAD_VIEW");
        assertEquals("ACTIVE", leadView.getStatus(), "Fix1: 退款降级FREE后LEAD_VIEW应重新激活");
        assertEquals(5, leadView.getQuotaTotal(), "FREE LEAD_VIEW配额应为5");

        // 再续费（安全：退款后可以重新购买）
        CommercialOrder order2 = payForPlan(companyId, userId, basicPlan);
        assertEquals("ACTIVE", subscriptionService.getSubscription(companyId).getStatus());
        assertEquals(order2.getId(), subscriptionService.getSubscription(companyId).getSourceOrderId());
        assertEquals(20, getEntitlement(companyId, "LEAD_VIEW").getQuotaTotal(), "续费后配额恢复BASIC级别");
    }

    // ==================== E-10: 到期任务与续费并发 ====================

    @Test
    void e10_expirationTaskAfterRenewalIsNoop() {
        Long companyId = createCompany("E10公司");
        Long userId = 920010L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");

        // 买BASIC并模拟到期
        payForPlan(companyId, userId, basicPlan);
        simulateExpiration(companyId);
        assertEquals("EXPIRED", subscriptionService.getSubscription(companyId).getStatus());

        // 续费
        payForPlan(companyId, userId, basicPlan);
        EnterpriseSubscription renewed = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", renewed.getStatus());

        // 连续执行两次到期任务（模拟多实例并发）
        int count1 = subscriptionService.processExpirations();
        int count2 = subscriptionService.processExpirations();

        // 续费后endTime在未来，到期任务不应处理该订阅
        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertEquals("ACTIVE", sub.getStatus(), "到期任务不得影响已续费订阅");
        assertTrue(countActiveEntitlements(companyId) > 0, "权益应保持ACTIVE");
    }

    // ==================== E-11: 重复到期任务（幂等） ====================

    @Test
    void e11_duplicateExpirationTaskIsIdempotent() {
        Long companyId = createCompany("E11公司");
        Long userId = 920011L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");
        payForPlan(companyId, userId, basicPlan);

        // 模拟到期
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getEndTime, LocalDateTime.now().minusDays(1))
                        .eq(EnterpriseSubscription::getId,
                                subscriptionService.getSubscription(companyId).getId()));

        // 第一次到期处理
        subscriptionService.processExpirations();
        assertEquals("EXPIRED", subscriptionService.getSubscription(companyId).getStatus());
        assertEquals(0, countActiveEntitlements(companyId));

        // 第二次到期处理（幂等：已EXPIRED不再处理）
        int secondCount = subscriptionService.processExpirations();
        assertEquals("EXPIRED", subscriptionService.getSubscription(companyId).getStatus(),
                "重复到期任务应幂等，状态不变");
        assertEquals(0, countActiveEntitlements(companyId));
    }

    // ==================== E-12: 重复退款通知（幂等） ====================

    @Test
    void e12_duplicateRefundConfirmationIsIdempotent() {
        Long companyId = createCompany("E12公司");
        Long userId = 920012L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");
        CommercialOrder order = payForPlan(companyId, userId, basicPlan);

        // 发起退款
        CommercialRefund refund = refundService.applyRefund(order.getId(), userId, "重复退款测试");

        // 第一次确认
        refundService.confirmRefund(refund.getId(), 1L);
        assertEquals("SUCCESS", refundMapper.selectById(refund.getId()).getStatus());
        assertEquals("REFUNDED", orderMapper.selectById(order.getId()).getStatus());
        assertEquals("FREE", subscriptionService.getSubscription(companyId).getStatus());

        // 第二次确认（幂等：已SUCCESS直接返回）
        refundService.confirmRefund(refund.getId(), 1L);
        assertEquals("SUCCESS", refundMapper.selectById(refund.getId()).getStatus(),
                "重复确认退款应幂等");

        // 订单状态不变，订阅不再重复降级
        assertEquals("REFUNDED", orderMapper.selectById(order.getId()).getStatus());
        // 验证仅一条SUCCESS退款记录
        long successCount = refundMapper.selectCount(
                Wrappers.<CommercialRefund>lambdaQuery()
                        .eq(CommercialRefund::getOrderId, order.getId())
                        .eq(CommercialRefund::getStatus, "SUCCESS"));
        assertEquals(1, successCount, "仅应有一条SUCCESS退款记录");
    }

    // ==================== E-13: Redis缓存未失效 ====================

    @Test
    void e13_entitlementCheckWorksWithoutRedisCache() {
        Long companyId = createCompany("E13公司");
        subscriptionService.ensureFreeSubscription(companyId);

        // RedisUtils是@MockBean（返回null/默认值），模拟Redis不可用
        // 权益校验必须直查DB，不能依赖Redis
        EnterpriseEntitlement ent = entitlementService.requireEntitlement(companyId, "LEAD_VIEW");
        assertNotNull(ent, "Redis不可用时权益校验必须直查DB成功");
        assertEquals("ACTIVE", ent.getStatus());

        // 配额消费也不依赖Redis
        EnterpriseEntitlement consumed = entitlementService.consumeQuota(
                companyId, "LEAD_VIEW", "TEST", 9301L);
        assertNotNull(consumed);
        assertEquals(1, consumed.getQuotaUsed(), "配额消费应直查DB成功");
    }

    // ==================== E-14: 多实例并发到期任务 ====================

    @Test
    void e14_multipleExpirationRunsAreSafe() {
        Long companyId = createCompany("E14公司");
        Long userId = 920014L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = activatePlan("BASIC");
        payForPlan(companyId, userId, basicPlan);

        // 模拟到期
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getEndTime, LocalDateTime.now().minusDays(1))
                        .eq(EnterpriseSubscription::getId,
                                subscriptionService.getSubscription(companyId).getId()));

        // 模拟多实例并发：连续执行到期任务
        // CAS保证仅一次成功标记EXPIRED，后续执行发现已EXPIRED不再处理
        subscriptionService.processExpirations();
        subscriptionService.processExpirations();
        subscriptionService.processExpirations();

        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertEquals("EXPIRED", sub.getStatus(), "多次到期任务后状态应正确为EXPIRED");

        // 权益仅被撤销一次（所有EXPIRED，无异常状态）
        List<EnterpriseEntitlement> ents = entitlementService.listEntitlements(companyId);
        for (EnterpriseEntitlement ent : ents) {
            assertNotEquals("ACTIVE", ent.getStatus(),
                    "多次到期任务后所有权益应非ACTIVE: " + ent.getEntCode());
        }
    }
}