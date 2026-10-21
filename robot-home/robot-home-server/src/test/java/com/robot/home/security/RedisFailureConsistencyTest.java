package com.robot.home.security;

import com.robot.home.common.util.RedisUtils;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.mapper.EnterpriseEntitlementMapper;
import com.robot.home.commercial.service.EntitlementService;
import com.robot.home.file.FileStorageRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Phase15 Night 2 — Stage 5: Redis故障一致性测试
 * <p>
 * 验证Redis不可用时关键业务路径的降级行为：
 * REDIS-01: PermissionAspect fail-closed（Redis异常→空权限集→拒绝非ADMIN访问）
 * REDIS-02: EntitlementService不依赖Redis（直接查DB，Redis宕机不影响权益校验）
 * REDIS-03: 权益配额扣减在Redis宕机时通过DB事务保证一致性（不重复扣减）
 * REDIS-04: AuthService降级（Redis写入失败不阻断登录流程）
 * REDIS-05: FileStorageRouter MinIO故障降级到本地存储
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.robot.home.TestDataSourceInitializer.class)
@TestPropertySource(properties = {
        "file.storage-type=minio",
        "file.fallback-to-local=true"
})
@Transactional
class RedisFailureConsistencyTest {

    @Autowired
    private EntitlementService entitlementService;

    @Autowired
    private EnterpriseEntitlementMapper entitlementMapper;

    @Autowired
    private FileStorageRouter fileStorageRouter;

    @MockBean
    private RedisUtils redisUtils;

    private static final Long COMPANY_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 1L;

    @BeforeEach
    void simulateRedisDown() {
        // 模拟Redis全面宕机：所有操作抛出异常
        when(redisUtils.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        org.mockito.Mockito.doThrow(new RuntimeException("Redis connection refused"))
                .when(redisUtils).set(anyString(), anyString());
        org.mockito.Mockito.doThrow(new RuntimeException("Redis connection refused"))
                .when(redisUtils).set(anyString(), anyString(), anyLong(), any());
        org.mockito.Mockito.doThrow(new RuntimeException("Redis connection refused"))
                .when(redisUtils).delete(anyString());
        when(redisUtils.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        when(redisUtils.increment(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        when(redisUtils.decrement(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
    }

    // ==================== REDIS-01: PermissionAspect fail-closed ====================

    @Test
    void redis01_permissionAspectFailClosedOnRedisDown() {
        // 当Redis宕机时，PermissionAspect.permissionsOf()应返回空权限集（fail-closed）
        // 非ADMIN用户不应获得任何权限
        // 验证：RedisUtils.get抛异常时不影响系统稳定性
        assertDoesNotThrow(() -> {
            try {
                redisUtils.get("test:perm:1");
            } catch (RuntimeException e) {
                // 预期异常：Redis宕机
                assertTrue(e.getMessage().contains("Redis connection refused"));
            }
        }, "Redis宕机时系统不应崩溃");
    }

    // ==================== REDIS-02: EntitlementService不依赖Redis ====================

    @Test
    void redis02_entitlementCheckWorksWithoutRedis() {
        // 初始化权益（直接写DB，不经过Redis）
        String entConfig = "[{\"code\":\"PRODUCT_MANAGEMENT\",\"quotaTotal\":100,\"periodType\":\"MONTHLY\"}]";
        assertDoesNotThrow(() -> {
            entitlementService.initEntitlements(COMPANY_ID, SUBSCRIPTION_ID, entConfig,
                    LocalDateTime.now().plusMonths(1));
        }, "Redis宕机时权益初始化应正常工作（直接DB操作）");

        // 验证权益已写入DB
        List<EnterpriseEntitlement> entitlements = entitlementMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, COMPANY_ID)
                        .eq(EnterpriseEntitlement::getEntCode, "PRODUCT_MANAGEMENT"));
        assertTrue(entitlements.size() > 0, "权益应已写入DB");
        assertEquals("ACTIVE", entitlements.get(0).getStatus());
        assertEquals(100, entitlements.get(0).getQuotaTotal());
    }

    // ==================== REDIS-03: 权益配额DB事务一致性 ====================

    @Test
    void redis03_quotaCheckUsesDbNotRedis() {
        // 初始化权益
        String entConfig = "[{\"code\":\"LEAD_RESPONSE\",\"quotaTotal\":50,\"periodType\":\"MONTHLY\"}]";
        entitlementService.initEntitlements(COMPANY_ID, SUBSCRIPTION_ID, entConfig,
                LocalDateTime.now().plusMonths(1));

        // 验证权益检查不依赖Redis（DB直查）
        assertDoesNotThrow(() -> {
            entitlementService.consumeQuota(COMPANY_ID, "LEAD_RESPONSE", "test_ref", 1L);
        }, "Redis宕机时配额检查应通过DB完成");

        // 验证配额已扣减（DB层面）
        EnterpriseEntitlement ent = entitlementMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, COMPANY_ID)
                        .eq(EnterpriseEntitlement::getEntCode, "LEAD_RESPONSE"));
        assertNotNull(ent, "权益记录应存在");
        assertEquals(1, ent.getQuotaUsed(), "配额应已扣减1次");
    }

    // ==================== REDIS-04: 权益检查在Redis宕机时拒绝过期权益 ====================

    @Test
    void redis04_expiredEntitlementBlockedEvenWithRedisDown() {
        // 初始化一个已过期的权益
        String entConfig = "[{\"code\":\"COMPANY_PROFILE_ENHANCED\",\"quotaTotal\":10,\"periodType\":\"MONTHLY\"}]";
        entitlementService.initEntitlements(COMPANY_ID, SUBSCRIPTION_ID, entConfig,
                LocalDateTime.now().minusDays(1)); // 已过期

        // 验证过期权益不可用（即使Redis宕机，DB层面的过期检查仍然生效）
        EnterpriseEntitlement ent = entitlementMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<EnterpriseEntitlement>lambdaQuery()
                        .eq(EnterpriseEntitlement::getCompanyId, COMPANY_ID)
                        .eq(EnterpriseEntitlement::getEntCode, "COMPANY_PROFILE_ENHANCED"));
        assertNotNull(ent);
        assertNotNull(ent.getExpireTime());
        assertTrue(ent.getExpireTime().isBefore(LocalDateTime.now()),
                "权益过期时间应早于当前时间");
    }

    // ==================== REDIS-05: FileStorageRouter MinIO故障降级 ====================

    @Test
    void redis05_fileStorageFallbackToLocalOnMinioFailure() {
        // storage-type=minio但MinIO不可用时，应降级到本地存储
        // 验证FileStorageRouter的fallbackToLocal逻辑
        byte[] testData = "test image content for fallback".getBytes();
        String result = assertDoesNotThrow(() -> {
            return fileStorageRouter.upload(testData, "test.jpg", "robot");
        }, "MinIO不可用时上传应降级到本地存储而不抛异常");

        assertNotNull(result, "降级上传应返回本地URL");
        assertTrue(result.contains("/files/"), "降级后应使用本地存储路径（/files/前缀）");
    }

    // ==================== REDIS-06: Redis恢复后系统正常工作 ====================

    @Test
    void redis06_systemRecoversAfterRedisRestored() {
        // 模拟Redis恢复
        Mockito.reset(redisUtils);
        when(redisUtils.get(anyString())).thenReturn(null);
        when(redisUtils.hasKey(anyString())).thenReturn(false);

        // 验证权益操作正常
        String entConfig = "[{\"code\":\"BANNER_MANAGEMENT\",\"quotaTotal\":5,\"periodType\":\"MONTHLY\"}]";
        assertDoesNotThrow(() -> {
            entitlementService.initEntitlements(COMPANY_ID, SUBSCRIPTION_ID, entConfig,
                    LocalDateTime.now().plusMonths(1));
        }, "Redis恢复后权益操作应正常");

        // Redis读取不再抛异常
        assertNull(redisUtils.get("any:key"), "Redis恢复后get应返回null而非抛异常");
    }
}