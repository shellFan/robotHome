package com.robot.home.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.advertising.entity.AdvertisingCampaign;
import com.robot.home.advertising.entity.AdvertisingSlot;
import com.robot.home.advertising.mapper.AdvertisingCampaignMapper;
import com.robot.home.advertising.mapper.AdvertisingSlotMapper;
import com.robot.home.advertising.service.AdvertisingService;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.entity.EnterpriseSubscription;
import com.robot.home.commercial.mapper.EnterpriseEntitlementMapper;
import com.robot.home.commercial.mapper.EnterpriseSubscriptionMapper;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import com.robot.home.commercial.service.EntitlementService;
import com.robot.home.message.entity.Message;
import com.robot.home.message.mapper.MessageMapper;
import com.robot.home.message.service.NotificationService;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase14 安全回归测试
 * <p>
 * 覆盖商业化安全红线：
 * 1. 广告jumpUrl危险协议拦截（javascript:/data:/vbscript:）
 * 2. 权益到期阻断（expired entitlement不可用）
 * 3. 广告总限额执行（totalLimit耗尽后不再展示）
 * 4. 广告日限额执行（dailyLimit耗尽后不再展示）
 * 5. 通知eventKey幂等（同一事件键只发送一次）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.robot.home.TestDataSourceInitializer.class)
@Transactional
class Phase14SecurityRegressionTest {

    @Autowired
    private AdvertisingService advertisingService;
    @Autowired
    private EntitlementService entitlementService;
    @Autowired
    private CommercialSubscriptionService subscriptionService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AdvertisingSlotMapper slotMapper;
    @Autowired
    private AdvertisingCampaignMapper campaignMapper;
    @Autowired
    private EnterpriseEntitlementMapper entitlementMapper;
    @Autowired
    private EnterpriseSubscriptionMapper subscriptionMapper;
    @Autowired
    private MessageMapper messageMapper;

    @MockBean
    private RedisUtils redisUtils;

    private static final Long COMPANY_ID = 1L;

    // ==================== 1. jumpUrl危险协议拦截 ====================

    @Test
    void jumpUrlDangerousProtocolsAreRejected() {
        AdvertisingSlot slot = slotMapper.selectOne(
                Wrappers.<AdvertisingSlot>lambdaQuery().eq(AdvertisingSlot::getSlotCode, "HOME_BANNER"));
        assertNotNull(slot, "测试库必须预置HOME_BANNER广告位");

        AdvertisingCampaign campaign = advertisingService.createCampaign(
                COMPANY_ID, slot.getId(), "安全测试投放",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(7),
                10000L, 0, 0);

        // javascript: 协议必须被拒绝
        assertThrows(BusinessException.class,
                () -> advertisingService.createCreative(campaign.getId(), COMPANY_ID, "IMAGE",
                        "https://cdn.example.invalid/ad.png", "javascript:alert(1)", "恶意广告", "xss测试"),
                "javascript:协议必须被拦截");

        // data: 协议必须被拒绝
        assertThrows(BusinessException.class,
                () -> advertisingService.createCreative(campaign.getId(), COMPANY_ID, "IMAGE",
                        "https://cdn.example.invalid/ad.png", "data:text/html,<script>alert(1)</script>", "恶意广告", "data测试"),
                "data:协议必须被拦截");

        // vbscript: 协议必须被拒绝
        assertThrows(BusinessException.class,
                () -> advertisingService.createCreative(campaign.getId(), COMPANY_ID, "IMAGE",
                        "https://cdn.example.invalid/ad.png", "vbscript:msgbox(1)", "恶意广告", "vbs测试"),
                "vbscript:协议必须被拦截");

        // 大小写绕过尝试（JavaScript:）必须被拒绝
        assertThrows(BusinessException.class,
                () -> advertisingService.createCreative(campaign.getId(), COMPANY_ID, "IMAGE",
                        "https://cdn.example.invalid/ad.png", "JavaScript:alert(1)", "恶意广告", "大小写绕过测试"),
                "大小写绕过的javascript:协议必须被拦截");

        // 正常https协议必须通过
        assertDoesNotThrow(() -> advertisingService.createCreative(campaign.getId(), COMPANY_ID, "IMAGE",
                "https://cdn.example.invalid/ad.png", "https://example.invalid/landing", "正常广告", "https测试"));

        // 正常http协议必须通过
        assertDoesNotThrow(() -> advertisingService.createCreative(campaign.getId(), COMPANY_ID, "IMAGE",
                "https://cdn.example.invalid/ad2.png", "http://example.invalid/landing", "正常广告", "http测试"));
    }

    // ==================== 2. 权益到期阻断 ====================

    @Test
    void expiredEntitlementIsNotUsable() {
        subscriptionService.ensureFreeSubscription(COMPANY_ID);
        EnterpriseSubscription sub = subscriptionService.getSubscription(COMPANY_ID);
        assertNotNull(sub);

        // 初始化一个已过期的权益
        entitlementService.initEntitlements(COMPANY_ID, sub.getId(),
                "[{\"code\":\"LEAD_VIEW\",\"quotaTotal\":10,\"periodType\":\"MONTHLY\"}]",
                LocalDateTime.now().minusDays(1)); // expireTime已过

        // checkEntitlement只返回ACTIVE状态的权益，过期后expireEntitlements会标记EXPIRED
        // 但即使状态仍为ACTIVE，requireEntitlement也会检查expireTime
        EnterpriseEntitlement ent = entitlementMapper.selectOne(
                Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, COMPANY_ID)
                        .eq(EnterpriseEntitlement::getEntCode, "LEAD_VIEW"));
        assertNotNull(ent, "权益应已初始化");

        // 手动将expireTime设为过去，模拟到期
        entitlementMapper.update(null,
                Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                        .set(EnterpriseEntitlement::getExpireTime, LocalDateTime.now().minusHours(1))
                        .eq(EnterpriseEntitlement::getId, ent.getId()));

        // requireEntitlement必须抛出"权益已过期"
        BusinessException ex = assertThrows(BusinessException.class,
                () -> entitlementService.requireEntitlement(COMPANY_ID, "LEAD_VIEW"),
                "已过期的权益必须被阻断");
        assertTrue(ex.getMessage().contains("过期"), "异常消息应包含'过期': " + ex.getMessage());

        // consumeQuota也必须失败（内部调用requireEntitlement）
        assertThrows(BusinessException.class,
                () -> entitlementService.consumeQuota(COMPANY_ID, "LEAD_VIEW", "TEST", 9999L),
                "已过期的权益不可消耗配额");
    }

    // ==================== 3. 广告总限额执行 ====================

    @Test
    void advertisingTotalLimitIsEnforced() {
        AdvertisingSlot slot = slotMapper.selectOne(
                Wrappers.<AdvertisingSlot>lambdaQuery().eq(AdvertisingSlot::getSlotCode, "ROBOT_LIST"));
        assertNotNull(slot, "测试库必须预置ROBOT_LIST广告位");

        // 创建totalLimit=1的投放计划
        AdvertisingCampaign campaign = advertisingService.createCampaign(
                COMPANY_ID, slot.getId(), "限额测试投放",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(7),
                10000L, 0, 1); // totalLimit=1

        advertisingService.createCreative(campaign.getId(), COMPANY_ID, "IMAGE",
                "https://cdn.example.invalid/ad.png", "https://example.invalid", "限额测试", "总限额1次");
        advertisingService.submitForReview(campaign.getId(), COMPANY_ID);
        advertisingService.approve(campaign.getId(), 1L, "审核通过");
        advertisingService.goOnline(campaign.getId(), COMPANY_ID);

        // 记录1次展示（达到totalLimit）
        advertisingService.recordEvent(campaign.getId(), slot.getId(), "IMPRESSION", "limit-visitor-001", "127.0.0.1");

        // 再次查询广告位展示，该计划不应出现（已达总限额）
        List<Map<String, Object>> ads = advertisingService.getSlotAds("ROBOT_LIST");
        boolean stillShowing = ads.stream()
                .anyMatch(ad -> campaign.getId().equals(ad.get("campaignId")));
        assertTrue(!stillShowing, "达到totalLimit的投放计划不应再展示");
    }

    // ==================== 4. 广告日限额执行 ====================

    @Test
    void advertisingDailyLimitIsEnforced() {
        AdvertisingSlot slot = slotMapper.selectOne(
                Wrappers.<AdvertisingSlot>lambdaQuery().eq(AdvertisingSlot::getSlotCode, "ARTICLE_DETAIL"));
        assertNotNull(slot, "测试库必须预置ARTICLE_DETAIL广告位");

        // 创建dailyLimit=1的投放计划（totalLimit=0不限）
        AdvertisingCampaign campaign = advertisingService.createCampaign(
                COMPANY_ID, slot.getId(), "日限额测试投放",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(7),
                10000L, 1, 0); // dailyLimit=1

        advertisingService.createCreative(campaign.getId(), COMPANY_ID, "IMAGE",
                "https://cdn.example.invalid/ad.png", "https://example.invalid", "日限额测试", "日限额1次");
        advertisingService.submitForReview(campaign.getId(), COMPANY_ID);
        advertisingService.approve(campaign.getId(), 1L, "审核通过");
        advertisingService.goOnline(campaign.getId(), COMPANY_ID);

        // 记录1次展示（达到dailyLimit）
        advertisingService.recordEvent(campaign.getId(), slot.getId(), "IMPRESSION", "daily-visitor-001", "127.0.0.1");

        // 再次查询广告位展示，该计划不应出现（已达日限额）
        List<Map<String, Object>> ads = advertisingService.getSlotAds("ARTICLE_DETAIL");
        boolean stillShowing = ads.stream()
                .anyMatch(ad -> campaign.getId().equals(ad.get("campaignId")));
        assertTrue(!stillShowing, "达到dailyLimit的投放计划当日不应再展示");
    }

    // ==================== 5. 通知eventKey幂等 ====================

    @Test
    void notificationEventKeyIsIdempotent() {
        Long userId = 920001L;
        String eventKey = "phase14-security-dedup-" + System.currentTimeMillis();

        // 第一次发送应成功
        notificationService.sendNotification(userId, "SYSTEM",
                "幂等测试通知", "第一次发送", "SYSTEM", null, eventKey);

        long countAfterFirst = messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getEventKey, eventKey));
        assertEquals(1, countAfterFirst, "第一次发送应产生1条消息");

        // 第二次发送同一eventKey应被去重跳过
        notificationService.sendNotification(userId, "SYSTEM",
                "幂等测试通知", "第二次发送", "SYSTEM", null, eventKey);

        long countAfterSecond = messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getEventKey, eventKey));
        assertEquals(1, countAfterSecond, "同一eventKey的重复通知必须被去重");

        // 不同eventKey应正常发送
        String otherEventKey = "phase14-security-dedup-other-" + System.currentTimeMillis();
        notificationService.sendNotification(userId, "SYSTEM",
                "另一条通知", "不同eventKey", "SYSTEM", null, otherEventKey);

        long countAfterOther = messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getEventKey, otherEventKey));
        assertEquals(1, countAfterOther, "不同eventKey的通知应正常发送");
    }
}