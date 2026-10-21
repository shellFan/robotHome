package com.robot.home;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterpriseSubscription;
import com.robot.home.commercial.mapper.CommercialOrderMapper;
import com.robot.home.commercial.mapper.EnterpriseEntitlementMapper;
import com.robot.home.commercial.mapper.EnterprisePlanMapper;
import com.robot.home.commercial.mapper.EnterpriseSubscriptionMapper;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import com.robot.home.commercial.service.EntitlementService;
import com.robot.home.commercial.service.PaymentService;
import com.robot.home.common.util.RedisUtils;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Night 4 Stage 5: 商业化定时任务测试
 * <p>
 * 补强CommercialScheduler三个定时任务的测试覆盖（此前为0覆盖GAP）：
 * 1. closeExpiredOrders — 订单超时关闭（30分钟未支付）
 * 2. processExpirations — 订阅到期处理（EXPIRING预警 + EXPIRED失效）
 * 3. resetMonthlyQuota — 月度配额重置
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
@Transactional
class CommercialSchedulerTest {

    @Autowired
    private CommercialOrderService orderService;
    @Autowired
    private CommercialSubscriptionService subscriptionService;
    @Autowired
    private EntitlementService entitlementService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CommercialOrderMapper orderMapper;
    @Autowired
    private EnterprisePlanMapper planMapper;
    @Autowired
    private EnterpriseSubscriptionMapper subscriptionMapper;
    @Autowired
    private EnterpriseEntitlementMapper entitlementMapper;

    @MockBean
    private RedisUtils redisUtils;

    private static final Long COMPANY_ID = 1L;

    /**
     * 准备BASIC套餐上架并创建PENDING_PAYMENT订单
     */
    private CommercialOrder preparePendingOrder(Long userId, String idemKey) {
        subscriptionService.ensureFreeSubscription(COMPANY_ID);
        EnterprisePlan basicPlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "BASIC"));
        assertNotNull(basicPlan, "测试库必须预置BASIC套餐");
        basicPlan.setStatus("ON_SHELF");
        basicPlan.setSaleable(1);
        planMapper.updateById(basicPlan);
        return orderService.createOrder(COMPANY_ID, userId, basicPlan.getId(), idemKey);
    }

    // ==================== 订单超时关闭 ====================

    @Test
    @DisplayName("SCHED-01: 30分钟前创建的PENDING_PAYMENT订单被关闭为EXPIRED")
    void closeExpiredOrders_closesOldPendingPayment() {
        CommercialOrder order = preparePendingOrder(920001L, "sched-expire-" + System.currentTimeMillis());
        assertEquals("PENDING_PAYMENT", order.getStatus());

        // 模拟订单创建于35分钟前
        orderMapper.update(null,
                Wrappers.<CommercialOrder>lambdaUpdate()
                        .set(CommercialOrder::getCreateTime, LocalDateTime.now().minusMinutes(35))
                        .eq(CommercialOrder::getId, order.getId()));

        int closed = orderService.closeExpiredOrders(30);
        assertTrue(closed >= 1, "应至少关闭1条超时订单");

        CommercialOrder after = orderMapper.selectById(order.getId());
        assertEquals("EXPIRED", after.getStatus(), "超时未支付订单必须变为EXPIRED");
        assertNotNull(after.getCloseTime(), "关闭时间必须被记录");
    }

    @Test
    @DisplayName("SCHED-02: 近期创建的PENDING_PAYMENT订单不被关闭")
    void closeExpiredOrders_keepsRecentOrders() {
        CommercialOrder order = preparePendingOrder(920002L, "sched-recent-" + System.currentTimeMillis());
        assertEquals("PENDING_PAYMENT", order.getStatus());

        // 订单刚创建（5分钟前），不应被关闭
        orderMapper.update(null,
                Wrappers.<CommercialOrder>lambdaUpdate()
                        .set(CommercialOrder::getCreateTime, LocalDateTime.now().minusMinutes(5))
                        .eq(CommercialOrder::getId, order.getId()));

        int closed = orderService.closeExpiredOrders(30);

        CommercialOrder after = orderMapper.selectById(order.getId());
        assertEquals("PENDING_PAYMENT", after.getStatus(), "未超时订单不应被关闭");
    }

    @Test
    @DisplayName("SCHED-03: 已支付订单即使超时也不被关闭")
    void closeExpiredOrders_keepsPaidOrders() {
        CommercialOrder order = preparePendingOrder(920003L, "sched-paid-" + System.currentTimeMillis());
        // 完整支付链路
        paymentService.createPayment(order.getId(), COMPANY_ID, "MOCK_SANDBOX");
        paymentService.sandboxConfirmPayment(order.getId(), COMPANY_ID);
        CommercialOrder paid = orderMapper.selectById(order.getId());
        assertEquals("PAID", paid.getStatus());

        // 即使createTime改为35分钟前，PAID订单不应被关闭
        orderMapper.update(null,
                Wrappers.<CommercialOrder>lambdaUpdate()
                        .set(CommercialOrder::getCreateTime, LocalDateTime.now().minusMinutes(35))
                        .eq(CommercialOrder::getId, order.getId()));

        orderService.closeExpiredOrders(30);

        CommercialOrder after = orderMapper.selectById(order.getId());
        assertEquals("PAID", after.getStatus(), "已支付订单不应被超时关闭");
    }

    // ==================== 订阅到期处理（EXPIRING预警） ====================

    @Test
    @DisplayName("SCHED-04: 7天内到期的ACTIVE订阅被标记为EXPIRING")
    void processExpirations_marksSoonToExpireAsExpiring() {
        // 完整支付链路获得ACTIVE订阅
        CommercialOrder order = preparePendingOrder(920004L, "sched-expiring-" + System.currentTimeMillis());
        paymentService.createPayment(order.getId(), COMPANY_ID, "MOCK_SANDBOX");
        paymentService.sandboxConfirmPayment(order.getId(), COMPANY_ID);

        EnterpriseSubscription sub = subscriptionService.getSubscription(COMPANY_ID);
        assertNotNull(sub);
        assertEquals("ACTIVE", sub.getStatus());

        // 设置endTime为3天后（在7天预警窗口内）
        subscriptionMapper.update(null,
                Wrappers.<EnterpriseSubscription>lambdaUpdate()
                        .set(EnterpriseSubscription::getEndTime, LocalDateTime.now().plusDays(3))
                        .eq(EnterpriseSubscription::getId, sub.getId()));

        subscriptionService.processExpirations();

        EnterpriseSubscription after = subscriptionMapper.selectById(sub.getId());
        assertEquals("EXPIRING", after.getStatus(), "7天内到期的ACTIVE订阅应被标记为EXPIRING");
    }

    // ==================== 月度配额重置 ====================

    @Test
    @DisplayName("SCHED-05: 上月周期的MONTHLY权益配额被重置且周期更新到本月")
    void resetMonthlyQuota_resetsPreviousMonthQuota() {
        // 直接构造一条上月周期的MONTHLY ACTIVE权益
        LocalDateTime lastMonthStart = LocalDateTime.now().minusMonths(1).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime lastMonthEnd = lastMonthStart.plusMonths(1);

        EnterpriseEntitlement ent = new EnterpriseEntitlement();
        ent.setCompanyId(COMPANY_ID);
        ent.setSubscriptionId(999L);
        ent.setEntCode("TEST_MONTHLY_QUOTA");
        ent.setQuotaTotal(100);
        ent.setQuotaUsed(42);
        ent.setPeriodType("MONTHLY");
        ent.setPeriodStart(lastMonthStart);
        ent.setPeriodEnd(lastMonthEnd);
        ent.setStatus("ACTIVE");
        entitlementMapper.insert(ent);

        // 执行月度配额重置
        entitlementService.resetMonthlyQuota(COMPANY_ID);

        EnterpriseEntitlement after = entitlementMapper.selectById(ent.getId());
        assertEquals(Integer.valueOf(0), after.getQuotaUsed(), "上月配额使用量必须被重置为0");
        assertNotNull(after.getPeriodStart());
        assertTrue(after.getPeriodStart().isAfter(lastMonthStart),
                "周期起始必须更新到本月");
    }

    @Test
    @DisplayName("SCHED-06: 本月已重置的MONTHLY权益不会被重复重置（幂等）")
    void resetMonthlyQuota_idempotentForCurrentMonth() {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        EnterpriseEntitlement ent = new EnterpriseEntitlement();
        ent.setCompanyId(COMPANY_ID);
        ent.setSubscriptionId(998L);
        ent.setEntCode("TEST_MONTHLY_CURRENT");
        ent.setQuotaTotal(100);
        ent.setQuotaUsed(15);
        ent.setPeriodType("MONTHLY");
        ent.setPeriodStart(monthStart);
        ent.setPeriodEnd(monthEnd);
        ent.setStatus("ACTIVE");
        entitlementMapper.insert(ent);

        entitlementService.resetMonthlyQuota(COMPANY_ID);

        EnterpriseEntitlement after = entitlementMapper.selectById(ent.getId());
        assertEquals(Integer.valueOf(15), after.getQuotaUsed(),
                "本月周期的权益不应被重置（periodStart >= monthStart被跳过）");
        assertEquals(monthStart, after.getPeriodStart(), "周期起始不应变化");
    }
}