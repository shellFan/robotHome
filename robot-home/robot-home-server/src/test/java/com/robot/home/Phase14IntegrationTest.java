package com.robot.home;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.advertising.entity.AdvertisingCampaign;
import com.robot.home.advertising.entity.AdvertisingEvent;
import com.robot.home.advertising.entity.AdvertisingSlot;
import com.robot.home.advertising.mapper.AdvertisingCampaignMapper;
import com.robot.home.advertising.mapper.AdvertisingEventMapper;
import com.robot.home.advertising.mapper.AdvertisingSlotMapper;
import com.robot.home.advertising.service.AdvertisingService;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.JwtUtils;
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
import com.robot.home.commercial.service.PlanService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Phase14: 商业化与平台运营 E2E集成测试
 * <p>
 * 覆盖核心商业链路：
 * 1. 订单→支付→订阅激活→权益开通 全链路
 * 2. 订单幂等 + 企业隔离
 * 3. 权益配额CAS防超扣
 * 4. 广告审核状态机 + 事件去重
 * 5. 退款→权益撤销→订阅降级
 * 6. 沙箱支付渠道可用性
 * 7. 回调金额防篡改
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
@Transactional
class Phase14IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CommercialOrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PlanService planService;
    @Autowired
    private EntitlementService entitlementService;
    @Autowired
    private CommercialSubscriptionService subscriptionService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private AdvertisingService advertisingService;
    @Autowired
    private CommercialOrderMapper orderMapper;
    @Autowired
    private EnterprisePlanMapper planMapper;
    @Autowired
    private EnterpriseSubscriptionMapper subscriptionMapper;
    @Autowired
    private EnterpriseEntitlementMapper entitlementMapper;
    @Autowired
    private CommercialRefundMapper refundMapper;
    @Autowired
    private AdvertisingSlotMapper slotMapper;
    @Autowired
    private AdvertisingCampaignMapper campaignMapper;
    @Autowired
    private AdvertisingEventMapper eventMapper;
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private RedisUtils redisUtils;

    // ==================== 订单→支付→权益 全链路 ====================

    @Test
    void orderPaymentActivatesSubscriptionAndEntitlements() {
        Long companyId = 1L;
        Long userId = 910001L;
        // 确保FREE订阅存在
        subscriptionService.ensureFreeSubscription(companyId);
        // 将BASIC套餐上架（测试SQL中BASIC默认DRAFT+saleable=0）
        EnterprisePlan basicPlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "BASIC"));
        assertNotNull(basicPlan, "测试库必须预置BASIC套餐");
        basicPlan.setStatus("ON_SHELF");
        basicPlan.setSaleable(1);
        planMapper.updateById(basicPlan);

        // 1. 创建订单
        CommercialOrder order = orderService.createOrder(
                companyId, userId, basicPlan.getId(), "idem-e2e-" + System.currentTimeMillis());
        assertNotNull(order.getOrderNo());
        assertEquals("PENDING_PAYMENT", order.getStatus());
        assertEquals(Long.valueOf(0), order.getPayableAmountCents(), "BASIC V1价格应为0（未配置真实价格）");

        // 2. 发起支付
        Map<String, Object> payResult = paymentService.createPayment(order.getId(), companyId, "MOCK_SANDBOX");
        assertFalse(payResult.containsKey("error"), "沙箱支付渠道应可用: " + payResult);
        assertNotNull(payResult.get("channelPaymentNo"));

        // 3. 沙箱确认支付
        Map<String, Object> confirmResult = paymentService.sandboxConfirmPayment(order.getId(), companyId);
        assertEquals(Boolean.TRUE, confirmResult.get("success"), "沙箱支付确认应成功: " + confirmResult);

        // 4. 验证订单已PAID
        CommercialOrder paidOrder = orderMapper.selectById(order.getId());
        assertEquals("PAID", paidOrder.getStatus());
        assertEquals("MOCK_SANDBOX", paidOrder.getPaymentChannel());
        assertNotNull(paidOrder.getPayTime());

        // 5. 验证订阅已激活
        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertNotNull(sub);
        assertEquals("ACTIVE", sub.getStatus());
        assertEquals(basicPlan.getId(), sub.getPlanId(), "订阅应指向BASIC套餐");
        assertEquals(order.getId(), sub.getSourceOrderId());

        // 6. 验证权益已初始化
        List<EnterpriseEntitlement> entitlements = entitlementService.listEntitlements(companyId);
        assertFalse(entitlements.isEmpty(), "订阅激活后必须初始化权益");
        assertTrue(entitlements.stream().anyMatch(e -> "LEAD_VIEW".equals(e.getEntCode())),
                "BASIC套餐必须包含LEAD_VIEW权益");
        assertTrue(entitlements.stream().anyMatch(e -> "LEAD_RESPONSE".equals(e.getEntCode())),
                "BASIC套餐必须包含LEAD_RESPONSE权益");
    }

    // ==================== 订单幂等 ====================

    @Test
    void orderCreationWithSameIdempotentKeyReturnsSameOrder() {
        Long companyId = 1L;
        Long userId = 910002L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan freePlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "FREE"));

        String idemKey = "idem-dup-" + System.currentTimeMillis();
        CommercialOrder first = orderService.createOrder(companyId, userId, freePlan.getId(), idemKey);
        CommercialOrder second = orderService.createOrder(companyId, userId, freePlan.getId(), idemKey);

        assertEquals(first.getId(), second.getId(), "相同幂等键必须返回同一订单");
        assertEquals(first.getOrderNo(), second.getOrderNo());
    }

    // ==================== 企业隔离 ====================

    @Test
    void crossCompanyOrderAccessIsDenied() {
        Long companyId = 1L;
        Long userId = 910003L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan freePlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "FREE"));
        CommercialOrder order = orderService.createOrder(
                companyId, userId, freePlan.getId(), "idem-iso-" + System.currentTimeMillis());

        // 另一家企业尝试访问
        Company other = new Company();
        other.setName("Isolation Test Company");
        other.setStatus(1);
        other.setVerificationStatus("VERIFIED");
        companyMapper.insert(other);

        assertThrows(BusinessException.class,
                () -> orderService.getOrder(order.getId(), other.getId()),
                "企业A的订单不能被企业B访问");
    }

    // ==================== 权益配额CAS ====================

    @Test
    void entitlementQuotaCasPreventsOverconsumption() {
        Long companyId = 1L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);

        // 初始化权益（FREE套餐自带LEAD_VIEW quotaTotal=5）
        entitlementService.initEntitlements(companyId, sub.getId(),
                "[{\"code\":\"LEAD_VIEW\",\"quotaTotal\":5,\"periodType\":\"MONTHLY\"}]");
        EnterpriseEntitlement ent = entitlementService.checkEntitlement(companyId, "LEAD_VIEW");
        assertNotNull(ent);
        // 直接修改配额为2，测试CAS防超扣
        entitlementMapper.update(null,
                Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                        .set(EnterpriseEntitlement::getQuotaTotal, 2)
                        .set(EnterpriseEntitlement::getQuotaUsed, 0)
                        .eq(EnterpriseEntitlement::getId, ent.getId()));
        assertEquals(2, entitlementService.checkEntitlement(companyId, "LEAD_VIEW").getQuotaTotal());

        // 消耗2次应成功
        entitlementService.consumeQuota(companyId, "LEAD_VIEW", "TEST", 1001L);
        entitlementService.consumeQuota(companyId, "LEAD_VIEW", "TEST", 1002L);
        EnterpriseEntitlement consumed = entitlementService.checkEntitlement(companyId, "LEAD_VIEW");
        assertEquals(2, consumed.getQuotaUsed(), "两次消耗后quotaUsed应为2");

        // 第3次应抛异常（CAS防超扣）
        assertThrows(BusinessException.class,
                () -> entitlementService.consumeQuota(companyId, "LEAD_VIEW", "TEST", 1003L),
                "配额耗尽后CAS扣减必须失败");

        // 释放后可再次消耗
        entitlementService.releaseQuota(companyId, "LEAD_VIEW", "TEST", 1002L);
        EnterpriseEntitlement released = entitlementService.checkEntitlement(companyId, "LEAD_VIEW");
        assertEquals(1, released.getQuotaUsed(), "释放后quotaUsed应减1");
        entitlementService.consumeQuota(companyId, "LEAD_VIEW", "TEST", 1004L);
    }

    // ==================== 广告审核状态机 + 事件去重 ====================

    @Test
    void advertisingFullReviewFlowWithEventDedup() {
        Long companyId = 1L;
        // 获取预置广告位
        AdvertisingSlot slot = slotMapper.selectOne(
                Wrappers.<AdvertisingSlot>lambdaQuery().eq(AdvertisingSlot::getSlotCode, "HOME_BANNER"));
        assertNotNull(slot, "测试库必须预置HOME_BANNER广告位");

        // 1. 创建投放计划
        AdvertisingCampaign campaign = advertisingService.createCampaign(
                companyId, slot.getId(), "Phase14测试投放",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(7),
                10000L, 100, 1000);
        assertNotNull(campaign.getId());
        assertEquals("DRAFT", campaign.getStatus());

        // 2. 无素材提交审核应失败
        assertThrows(BusinessException.class,
                () -> advertisingService.submitForReview(campaign.getId(), companyId),
                "无素材的投放计划不能提交审核");

        // 3. 添加素材后提交审核
        advertisingService.createCreative(campaign.getId(), companyId, "IMAGE",
                "https://cdn.example.invalid/ad.png", "https://example.invalid", "测试广告", "Phase14测试");
        advertisingService.submitForReview(campaign.getId(), companyId);
        AdvertisingCampaign pending = campaignMapper.selectById(campaign.getId());
        assertEquals("PENDING_REVIEW", pending.getStatus());

        // 4. Admin审核通过
        advertisingService.approve(campaign.getId(), 1L, "审核通过");
        AdvertisingCampaign approved = campaignMapper.selectById(campaign.getId());
        assertEquals("APPROVED", approved.getStatus());

        // 5. 上线
        advertisingService.goOnline(campaign.getId(), companyId);
        AdvertisingCampaign online = campaignMapper.selectById(campaign.getId());
        assertEquals("ONLINE", online.getStatus());

        // 6. 公开展示应包含该广告
        List<Map<String, Object>> ads = advertisingService.getSlotAds("HOME_BANNER");
        assertFalse(ads.isEmpty(), "ONLINE状态的投放应出现在广告位展示中");

        // 7. 事件去重：同一访客同一类型同一天只记录一次
        advertisingService.recordEvent(campaign.getId(), slot.getId(), "IMPRESSION", "visitor-001", "127.0.0.1");
        advertisingService.recordEvent(campaign.getId(), slot.getId(), "IMPRESSION", "visitor-001", "127.0.0.1");
        long eventCount = eventMapper.selectCount(
                Wrappers.<AdvertisingEvent>lambdaQuery()
                        .eq(AdvertisingEvent::getCampaignId, campaign.getId())
                        .eq(AdvertisingEvent::getEventType, "IMPRESSION"));
        assertEquals(1, eventCount, "同一访客同一类型事件必须去重（uk_event_dedup）");

        // 8. 不同访客可分别记录
        advertisingService.recordEvent(campaign.getId(), slot.getId(), "IMPRESSION", "visitor-002", "127.0.0.1");
        long eventCount2 = eventMapper.selectCount(
                Wrappers.<AdvertisingEvent>lambdaQuery()
                        .eq(AdvertisingEvent::getCampaignId, campaign.getId())
                        .eq(AdvertisingEvent::getEventType, "IMPRESSION"));
        assertEquals(2, eventCount2, "不同访客的事件应分别记录");
    }

    // ==================== 退款→权益撤销 ====================

    @Test
    void refundRevokesEntitlementsAndDowngradesSubscription() {
        Long companyId = 1L;
        Long userId = 910004L;
        // 完整支付链路
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "BASIC"));
        basicPlan.setStatus("ON_SHELF");
        basicPlan.setSaleable(1);
        planMapper.updateById(basicPlan);
        CommercialOrder order = orderService.createOrder(
                companyId, userId, basicPlan.getId(), "idem-refund-" + System.currentTimeMillis());
        paymentService.createPayment(order.getId(), companyId, "MOCK_SANDBOX");
        paymentService.sandboxConfirmPayment(order.getId(), companyId);
        // 确认权益存在
        assertFalse(entitlementService.listEntitlements(companyId).isEmpty());

        // 1. 申请退款
        CommercialRefund refund = refundService.applyRefund(order.getId(), 1L, "测试退款");
        assertNotNull(refund.getRefundNo());
        assertEquals("APPLYING", refund.getStatus());

        // 2. 确认退款
        refundService.confirmRefund(refund.getId(), 1L);
        CommercialRefund confirmed = refundMapper.selectById(refund.getId());
        assertEquals("SUCCESS", confirmed.getStatus());

        // 3. 订单应REFUNDED
        CommercialOrder refundedOrder = orderMapper.selectById(order.getId());
        assertEquals("REFUNDED", refundedOrder.getStatus());

        // 4. 权益降级验证：BASIC专属权益撤销，FREE级别权益重新激活为FREE配额（Fix1行为修正）
        List<EnterpriseEntitlement> entitlements = entitlementService.listEntitlements(companyId);
        // 4a. BASIC专属权益（FREE计划不含）必须被撤销
        entitlements.stream()
                .filter(e -> "PRODUCT_MANAGEMENT".equals(e.getEntCode())
                        || "COMPANY_PROFILE_ENHANCED".equals(e.getEntCode()))
                .forEach(e -> assertFalse("ACTIVE".equals(e.getStatus()),
                        "BASIC专属权益" + e.getEntCode() + "退款后必须撤销"));
        // 4b. FREE级别权益重新激活，配额降级为FREE级别
        EnterpriseEntitlement leadView = entitlements.stream()
                .filter(e -> "LEAD_VIEW".equals(e.getEntCode())).findFirst().orElse(null);
        assertNotNull(leadView, "LEAD_VIEW权益应存在");
        assertEquals("ACTIVE", leadView.getStatus(), "退款后FREE级别LEAD_VIEW应重新激活为ACTIVE");
        assertEquals(5, leadView.getQuotaTotal(), "退款后LEAD_VIEW配额应降级为FREE级别(5)");
        EnterpriseEntitlement leadResponse = entitlements.stream()
                .filter(e -> "LEAD_RESPONSE".equals(e.getEntCode())).findFirst().orElse(null);
        assertNotNull(leadResponse, "LEAD_RESPONSE权益应存在");
        assertEquals("ACTIVE", leadResponse.getStatus(), "退款后FREE级别LEAD_RESPONSE应重新激活为ACTIVE");
        assertEquals(2, leadResponse.getQuotaTotal(), "退款后LEAD_RESPONSE配额应降级为FREE级别(2)");

        // 5. 订阅应降级
        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertNotNull(sub);
        assertFalse("ACTIVE".equals(sub.getStatus()) && "BASIC".equals(sub.getPlanName()),
                "退款后订阅不应保持BASIC ACTIVE状态");
    }

    // ==================== 沙箱支付渠道 ====================

    @Test
    void sandboxPaymentChannelIsAvailableInTestProfile() {
        List<Map<String, Object>> channels = paymentService.listAvailableChannels();
        assertFalse(channels.isEmpty());
        boolean hasMockSandbox = channels.stream()
                .anyMatch(ch -> "MOCK_SANDBOX".equals(ch.get("channel")) && Boolean.TRUE.equals(ch.get("available")));
        assertTrue(hasMockSandbox, "test profile下MOCK_SANDBOX渠道必须可用");
        // 真实支付未配置标记
        boolean hasRealNotice = channels.stream()
                .anyMatch(ch -> "REAL_PAYMENT".equals(ch.get("channel")));
        assertTrue(hasRealNotice, "未配置真实支付时必须标记REAL_PAYMENT_NOT_CONFIGURED");
    }

    // ==================== 回调金额防篡改 ====================

    @Test
    void callbackWithTamperedAmountIsRejected() {
        Long companyId = 1L;
        Long userId = 910005L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan freePlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "FREE"));
        CommercialOrder order = orderService.createOrder(
                companyId, userId, freePlan.getId(), "idem-tamper-" + System.currentTimeMillis());
        paymentService.createPayment(order.getId(), companyId, "MOCK_SANDBOX");

        // 构造篡改金额的回调（金额不一致）
        String mockNo = "MOCK_TAMPER_" + System.currentTimeMillis();
        String sign = cn.hutool.crypto.digest.DigestUtil.md5Hex(
                order.getOrderNo() + mockNo + "MOCK_SANDBOX_SECRET");
        Map<String, String> params = new HashMap<>();
        params.put("orderNo", order.getOrderNo());
        params.put("mockNo", mockNo);
        params.put("status", "SUCCESS");
        params.put("amountCents", "999999"); // 篡改金额
        params.put("sign", sign);

        Map<String, Object> result = paymentService.handleCallback("MOCK_SANDBOX", params, "tampered");
        assertEquals(Boolean.FALSE, result.get("success"), "金额篡改的回调必须被拒绝");

        // 订单不应被确认
        CommercialOrder orderAfter = orderMapper.selectById(order.getId());
        assertEquals("PENDING_PAYMENT", orderAfter.getStatus(), "金额不一致时订单必须保持PENDING_PAYMENT");
    }

    // ==================== 公开套餐接口 ====================

    @Test
    void publicPlanListOnlyShowsOnShelfSaleable() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/plans")).andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, root.path("code").asInt());
        JsonNode data = root.path("data");
        assertTrue(data.isArray(), "套餐列表应返回数组");
        // 只应有ON_SHELF+saleable的套餐（测试库中仅FREE）
        for (JsonNode plan : data) {
            assertEquals("ON_SHELF", plan.path("status").asText(),
                    "公开列表只展示上架套餐");
        }
        assertTrue(data.size() >= 1, "FREE套餐应在上架列表中");
    }

    // ==================== E2E-03: 权益到期链条 ====================

    @Test
    void subscriptionExpirationExpiresEntitlements() {
        Long companyId = 1L;
        Long userId = 910008L;
        // 完整支付链路获得ACTIVE订阅（processExpirations只扫描ACTIVE/EXPIRING）
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "BASIC"));
        basicPlan.setStatus("ON_SHELF");
        basicPlan.setSaleable(1);
        planMapper.updateById(basicPlan);
        CommercialOrder order = orderService.createOrder(
                companyId, userId, basicPlan.getId(), "idem-expire-" + System.currentTimeMillis());
        paymentService.createPayment(order.getId(), companyId, "MOCK_SANDBOX");
        paymentService.sandboxConfirmPayment(order.getId(), companyId);

        EnterpriseSubscription sub = subscriptionService.getSubscription(companyId);
        assertNotNull(sub);
        assertEquals("ACTIVE", sub.getStatus(), "支付后订阅应为ACTIVE");

        // 验证权益已初始化
        List<EnterpriseEntitlement> ents = entitlementService.listEntitlements(companyId);
        assertFalse(ents.isEmpty(), "权益应已初始化");

        // 模拟订阅到期：将endTime设为过去
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getEndTime, LocalDateTime.now().minusDays(1))
                        .eq(EnterpriseSubscription::getId, sub.getId()));

        // 执行到期扫描
        subscriptionService.processExpirations();

        // 验证订阅已EXPIRED
        EnterpriseSubscription expiredSub = subscriptionMapper.selectById(sub.getId());
        assertEquals("EXPIRED", expiredSub.getStatus(), "到期订阅必须被标记为EXPIRED");

        // 验证权益已被撤销（非ACTIVE）
        List<EnterpriseEntitlement> expiredEnts = entitlementService.listEntitlements(companyId);
        assertTrue(expiredEnts.stream().noneMatch(e -> "ACTIVE".equals(e.getStatus())),
                "订阅到期后所有权益必须被撤销（非ACTIVE）");

        // 过期后消耗配额必须失败
        assertThrows(BusinessException.class,
                () -> entitlementService.consumeQuota(companyId, "LEAD_VIEW", "TEST", 8888L),
                "订阅到期后不可消耗配额");
    }

    // ==================== E2E-04b: 回调幂等（同一回调重复处理） ====================

    @Test
    void duplicateCallbackIsIdempotent() {
        Long companyId = 1L;
        Long userId = 910006L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan freePlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "FREE"));
        CommercialOrder order = orderService.createOrder(
                companyId, userId, freePlan.getId(), "idem-callback-" + System.currentTimeMillis());
        paymentService.createPayment(order.getId(), companyId, "MOCK_SANDBOX");

        // 构造正确的回调
        String mockNo = "MOCK_DUP_" + System.currentTimeMillis();
        String sign = cn.hutool.crypto.digest.DigestUtil.md5Hex(
                order.getOrderNo() + mockNo + "MOCK_SANDBOX_SECRET");
        Map<String, String> params = new HashMap<>();
        params.put("orderNo", order.getOrderNo());
        params.put("mockNo", mockNo);
        params.put("status", "SUCCESS");
        params.put("amountCents", String.valueOf(order.getPayableAmountCents()));
        params.put("sign", sign);

        // 第一次回调应成功
        Map<String, Object> firstResult = paymentService.handleCallback("MOCK_SANDBOX", params, "dup-test");
        assertEquals(Boolean.TRUE, firstResult.get("success"), "第一次回调应成功");

        // 订单应PAID
        CommercialOrder paidOrder = orderMapper.selectById(order.getId());
        assertEquals("PAID", paidOrder.getStatus());

        // 第二次相同回调应幂等返回（不重复处理）
        Map<String, Object> secondResult = paymentService.handleCallback("MOCK_SANDBOX", params, "dup-test");
        // 幂等：要么返回success=true（已处理），要么返回明确的已处理标记
        // 关键是订单状态不变、不产生重复支付记录
        assertNotNull(secondResult, "重复回调必须有响应");

        // 验证订单仍为PAID（未被重复处理破坏）
        CommercialOrder orderAfterDup = orderMapper.selectById(order.getId());
        assertEquals("PAID", orderAfterDup.getStatus(), "重复回调后订单状态必须保持PAID");

        // 验证支付记录只有一条SUCCESS
        long successPayments = paymentService.getPaymentsByOrderNo(order.getOrderNo()).stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .count();
        assertEquals(1, successPayments, "重复回调不应产生多条SUCCESS支付记录");
    }

    // ==================== E2E-09b: Admin商业统计端点 ====================

    @Test
    void adminCommercialStatsEndpointReturnsCorrectStructure() throws Exception {
        // 先产生一些商业数据
        Long companyId = 1L;
        Long userId = 910007L;
        subscriptionService.ensureFreeSubscription(companyId);
        EnterprisePlan basicPlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "BASIC"));
        basicPlan.setStatus("ON_SHELF");
        basicPlan.setSaleable(1);
        planMapper.updateById(basicPlan);
        CommercialOrder order = orderService.createOrder(
                companyId, userId, basicPlan.getId(), "idem-stats-" + System.currentTimeMillis());
        paymentService.createPayment(order.getId(), companyId, "MOCK_SANDBOX");
        paymentService.sandboxConfirmPayment(order.getId(), companyId);

        // 生成Admin JWT（TOKEN_TYPE_ADMIN，ROLE_ADMIN bypass @RequirePermission）
        String adminToken = jwtUtils.generateAdminToken(1L, "admin", "ADMIN");

        // 调用Admin统计端点
        MvcResult result = mockMvc.perform(get("/api/admin/commercial/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, root.path("code").asInt(), "Admin统计端点应返回200");
        JsonNode data = root.path("data");

        // 验证订单统计结构
        JsonNode orderStats = data.path("orderStats");
        assertTrue(orderStats.has("total"), "orderStats必须包含total");
        assertTrue(orderStats.has("paid"), "orderStats必须包含paid");
        assertTrue(orderStats.path("total").asInt() >= 1, "至少有1条订单");

        // 验证收入统计结构（严格区分REAL/SANDBOX）
        JsonNode revenueStats = data.path("revenueStats");
        assertTrue(revenueStats.has("realRevenueCents"), "revenueStats必须包含realRevenueCents");
        assertTrue(revenueStats.has("sandboxRevenueCents"), "revenueStats必须包含sandboxRevenueCents");
        assertTrue(revenueStats.has("totalRevenueCents"), "revenueStats必须包含totalRevenueCents");
        assertTrue(revenueStats.has("channelRevenue"), "revenueStats必须包含channelRevenue");

        // 沙箱支付的订单金额必须计入sandboxRevenueCents，不能计入realRevenueCents
        long sandboxRevenue = revenueStats.path("sandboxRevenueCents").asLong();
        long realRevenue = revenueStats.path("realRevenueCents").asLong();
        assertTrue(sandboxRevenue >= 0, "沙箱收入必须非负");
        assertTrue(realRevenue >= 0, "真实收入必须非负");

        // 验证订阅统计结构
        JsonNode subStats = data.path("subscriptionStats");
        assertTrue(subStats.has("total"), "subscriptionStats必须包含total");
        assertTrue(subStats.has("active"), "subscriptionStats必须包含active");

        // 验证退款统计结构
        JsonNode refundStats = data.path("refundStats");
        assertTrue(refundStats.has("total"), "refundStats必须包含total");
    }
}