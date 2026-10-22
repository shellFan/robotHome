package com.robot.home;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterpriseSubscription;
import com.robot.home.commercial.mapper.CommercialOrderMapper;
import com.robot.home.commercial.mapper.EnterpriseEntitlementMapper;
import com.robot.home.commercial.mapper.EnterprisePlanMapper;
import com.robot.home.commercial.mapper.EnterpriseSubscriptionMapper;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.EntitlementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase15 Night 2 — Stage 6: 商业化故障恢复测试
 * <p>
 * 覆盖COM-01~COM-10中尚未覆盖的关键场景：
 * COM-01: 重复支付确认不重复激活订阅（幂等）
 * COM-03: 取消与支付确认竞争（CAS状态机保证只有一个胜出）
 * COM-07: 支付超时订单可取消（PENDING_PAYMENT超时后可安全取消）
 * COM-08: 月度配额重置（resetMonthlyQuota功能验证）
 * COM-09: 并发创建同幂等键订单（返回同一订单，不重复创建）
 * COM-10: 重复取消已取消订单（CAS失败，抛出异常）
 * <p>
 * 注意：不使用@Transactional——并发测试需要已提交数据对其他线程可见。
 * 使用唯一companyId隔离测试数据，@AfterEach负责清理。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.robot.home.TestDataSourceInitializer.class)
class CommercialFailureRecoveryTest {

    @Autowired
    private CommercialOrderService orderService;
    @Autowired
    private EntitlementService entitlementService;
    @Autowired
    private CommercialOrderMapper orderMapper;
    @Autowired
    private EnterpriseSubscriptionMapper subscriptionMapper;
    @Autowired
    private EnterpriseEntitlementMapper entitlementMapper;
    @Autowired
    private EnterprisePlanMapper planMapper;

    @MockBean
    private RedisUtils redisUtils;

    private static final long USER_ID = 910001L;
    private static final AtomicLong COMPANY_ID_SEQ = new AtomicLong(920000L);

    private Long testCompanyId;
    private Long planId;

    @BeforeEach
    void setUp() {
        // 每个测试使用唯一companyId，避免数据干扰
        testCompanyId = COMPANY_ID_SEQ.incrementAndGet();

        // 将BASIC套餐上架（测试SQL中BASIC默认DRAFT+saleable=0）
        EnterprisePlan basicPlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "BASIC"));
        assertNotNull(basicPlan, "测试库必须预置BASIC套餐");
        basicPlan.setStatus("ON_SHELF");
        basicPlan.setSaleable(1);
        planMapper.updateById(basicPlan);
        planId = basicPlan.getId();
    }

    @AfterEach
    void tearDown() {
        // 清理本测试创建的数据
        if (testCompanyId != null) {
            orderMapper.delete(Wrappers.<CommercialOrder>lambdaQuery()
                    .eq(CommercialOrder::getCompanyId, testCompanyId));
            subscriptionMapper.delete(Wrappers.<EnterpriseSubscription>lambdaQuery()
                    .eq(EnterpriseSubscription::getCompanyId, testCompanyId));
            entitlementMapper.delete(Wrappers.<EnterpriseEntitlement>lambdaQuery()
                    .eq(EnterpriseEntitlement::getCompanyId, testCompanyId));
        }
        // 恢复BASIC套餐为DRAFT（避免影响其他测试）
        EnterprisePlan basicPlan = planMapper.selectOne(
                Wrappers.<EnterprisePlan>lambdaQuery().eq(EnterprisePlan::getCode, "BASIC"));
        if (basicPlan != null) {
            basicPlan.setStatus("DRAFT");
            basicPlan.setSaleable(0);
            planMapper.updateById(basicPlan);
        }
    }

    // ==================== COM-03: 取消与支付确认竞争 ====================

    @Test
    void com03_cancelAndPaymentConfirmRace_onlyOneWins() {
        // 创建订单
        CommercialOrder order = orderService.createOrder(testCompanyId, USER_ID, planId,
                "com03-race-" + System.currentTimeMillis());
        assertNotNull(order);

        // 并发执行取消和支付确认
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger cancelSuccess = new AtomicInteger(0);
        AtomicInteger paySuccess = new AtomicInteger(0);
        AtomicInteger exceptions = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                orderService.cancelOrder(order.getId(), testCompanyId);
                cancelSuccess.incrementAndGet();
            } catch (Exception e) {
                exceptions.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                orderService.confirmPayment(order.getId(), "MOCK_SANDBOX", "pay-" + System.currentTimeMillis());
                paySuccess.incrementAndGet();
            } catch (Exception e) {
                exceptions.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();

        // 验证：取消和支付确认只有一个成功（CAS状态机保证）
        CommercialOrder finalOrder = orderMapper.selectById(order.getId());
        assertNotNull(finalOrder);
        String finalStatus = finalOrder.getStatus();
        // 最终状态必须是CANCELLED或PAID之一，不能是PENDING_PAYMENT
        assertTrue("CANCELLED".equals(finalStatus) || "PAID".equals(finalStatus),
                "最终状态应为CANCELLED或PAID，实际: " + finalStatus);
        assertEquals(1, cancelSuccess.get() + paySuccess.get(),
                "取消和支付确认应只有一个成功，cancel=" + cancelSuccess.get() + ", pay=" + paySuccess.get());
    }

    // ==================== COM-07: 支付超时订单可取消 ====================

    @Test
    void com07_pendingPaymentOrderCanBeCancelledAfterTimeout() {
        // 创建订单（PENDING_PAYMENT状态）
        CommercialOrder order = orderService.createOrder(testCompanyId, USER_ID, planId,
                "com07-timeout-" + System.currentTimeMillis());
        assertNotNull(order);
        assertEquals("PENDING_PAYMENT", order.getStatus());

        // 模拟超时：直接取消（实际场景由定时任务触发）
        orderService.cancelOrder(order.getId(), testCompanyId);

        // 验证已取消
        CommercialOrder cancelled = orderMapper.selectById(order.getId());
        assertEquals("CANCELLED", cancelled.getStatus());
        assertNotNull(cancelled.getCloseTime());

        // 验证取消后不能再支付确认
        assertThrows(BusinessException.class, () -> {
            orderService.confirmPayment(order.getId(), "MOCK_SANDBOX", "pay-late");
        }, "已取消的订单不应能支付确认");
    }

    // ==================== COM-08: 月度配额重置 ====================

    @Test
    void com08_monthlyQuotaResetRestoresUsedQuota() {
        // 初始化权益
        String entConfig = "[{\"code\":\"LEAD_RESPONSE\",\"quotaTotal\":30,\"periodType\":\"MONTHLY\"}]";
        entitlementService.initEntitlements(testCompanyId, 1L, entConfig,
                LocalDateTime.now().plusMonths(1));

        // 消耗一些配额
        entitlementService.consumeQuota(testCompanyId, "LEAD_RESPONSE", "test", 1L);
        entitlementService.consumeQuota(testCompanyId, "LEAD_RESPONSE", "test", 2L);

        EnterpriseEntitlement before = entitlementMapper.selectOne(
                Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, testCompanyId)
                        .eq(EnterpriseEntitlement::getEntCode, "LEAD_RESPONSE"));
        assertEquals(2, before.getQuotaUsed(), "消耗后配额应=2");

        // 模拟上月周期：手动将periodStart设为上月（resetMonthlyQuota只重置periodStart<本月1日的记录）
        LocalDateTime lastMonthStart = LocalDateTime.now().minusMonths(1)
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        entitlementMapper.update(null,
                Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                        .set(EnterpriseEntitlement::getPeriodStart, lastMonthStart)
                        .eq(EnterpriseEntitlement::getId, before.getId()));

        // 执行月度重置
        entitlementService.resetMonthlyQuota(testCompanyId);

        // 验证配额已重置
        EnterpriseEntitlement after = entitlementMapper.selectOne(
                Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, testCompanyId)
                        .eq(EnterpriseEntitlement::getEntCode, "LEAD_RESPONSE"));
        assertEquals(0, after.getQuotaUsed(), "重置后配额应=0");
        assertNotNull(after.getPeriodStart(), "重置后周期开始应更新");
        assertNotNull(after.getPeriodEnd(), "重置后周期结束应更新");
    }

    // ==================== COM-09: 并发创建同幂等键订单 ====================

    @Test
    void com09_concurrentCreateWithSameIdempotentKeyReturnsSameOrder() {
        String idempotentKey = "com09-concurrent-" + System.currentTimeMillis();

        // 并发创建3个同幂等键的订单
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    CommercialOrder order = orderService.createOrder(testCompanyId, USER_ID, planId, idempotentKey);
                    if (order != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 并发时可能抛出DuplicateKeyException，这是预期行为
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();

        // 验证：幂等键只产生一个订单
        List<CommercialOrder> orders = orderMapper.selectList(
                Wrappers.<CommercialOrder>lambdaQuery()
                        .eq(CommercialOrder::getIdempotentKey, idempotentKey));
        assertEquals(1, orders.size(), "同幂等键应只产生1个订单，实际: " + orders.size());
        assertTrue(successCount.get() >= 1, "至少1个线程应成功创建订单");
    }

    // ==================== COM-10: 重复取消已取消订单 ====================

    @Test
    void com10_doubleCancelThrowsException() {
        // 创建并取消订单
        CommercialOrder order = orderService.createOrder(testCompanyId, USER_ID, planId,
                "com10-double-cancel-" + System.currentTimeMillis());
        orderService.cancelOrder(order.getId(), testCompanyId);

        // 再次取消应失败（CAS：状态已不是CREATED/PENDING_PAYMENT）
        assertThrows(BusinessException.class, () -> {
            orderService.cancelOrder(order.getId(), testCompanyId);
        }, "重复取消应抛出异常");

        // 验证状态仍为CANCELLED
        CommercialOrder finalOrder = orderMapper.selectById(order.getId());
        assertEquals("CANCELLED", finalOrder.getStatus());
    }

    // ==================== COM-01补充: 重复回调不重复激活订阅 ====================

    @Test
    void com01_duplicatePaymentConfirmDoesNotDoubleActivate() {
        // 创建订单并支付
        CommercialOrder order = orderService.createOrder(testCompanyId, USER_ID, planId,
                "com01-dup-" + System.currentTimeMillis());
        orderService.confirmPayment(order.getId(), "MOCK_SANDBOX", "pay-dup-001");

        // 统计订阅数量
        long subCountAfterFirst = subscriptionMapper.selectCount(
                Wrappers.<EnterpriseSubscription>lambdaQuery()
                        .eq(EnterpriseSubscription::getCompanyId, testCompanyId)
                        .eq(EnterpriseSubscription::getStatus, "ACTIVE"));

        // 重复支付确认（幂等返回）
        orderService.confirmPayment(order.getId(), "MOCK_SANDBOX", "pay-dup-001");

        // 验证订阅数量未增加
        long subCountAfterSecond = subscriptionMapper.selectCount(
                Wrappers.<EnterpriseSubscription>lambdaQuery()
                        .eq(EnterpriseSubscription::getCompanyId, testCompanyId)
                        .eq(EnterpriseSubscription::getStatus, "ACTIVE"));
        assertEquals(subCountAfterFirst, subCountAfterSecond,
                "重复支付确认不应重复激活订阅");
    }
}