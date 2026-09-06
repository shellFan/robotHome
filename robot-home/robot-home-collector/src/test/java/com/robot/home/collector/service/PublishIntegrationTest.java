package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.robot.home.collector.CollectorApplication;
import com.robot.home.collector.CollectorTestDataSourceInitializer;
import com.robot.home.collector.entity.*;
import com.robot.home.collector.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * 发布服务数据库集成测试
 * 使用 @SpringBootTest + H2 内存数据库，验证真实 SQL 执行和事务行为。
 *
 * 注意：REQUIRES_NEW 事务方法在独立事务中执行，测试方法不能使用 @Transactional
 * （否则内层事务无法看到外层未提交的数据）。
 * 使用 @BeforeEach 清理测试数据，替代 @DirtiesContext 避免上下文重建开销。
 */
@SpringBootTest(
        classes = {CollectorApplication.class, CollectorTestDataSourceInitializer.class, PublishIntegrationTest.TestConfig.class},
        properties = {
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("发布服务数据库集成测试")
public class PublishIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

        @Bean
        @Primary
        public StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }
    }

    @Autowired
    private PublishTransactionService txService;

    @Autowired
    private CrawlerArticleMapper crawlerArticleMapper;

    @Autowired
    private CrawlerProductMapper crawlerProductMapper;

    @Autowired
    private PublishArticleMapper publishArticleMapper;

    @Autowired
    private PublishRobotMapper publishRobotMapper;

    @Autowired
    private PublishRobotImageMapper publishRobotImageMapper;

    @Autowired
    private PublishRobotPriceMapper publishRobotPriceMapper;

    @Autowired
    private PublishRobotTagMapper publishRobotTagMapper;

    @Autowired
    private PublishRobotVideoMapper publishRobotVideoMapper;

    @Autowired
    private PublishRobotParamValueMapper publishRobotParamValueMapper;

    @Autowired
    private PublishRobotParamDefMapper publishRobotParamDefMapper;

    /**
     * 每个测试方法前清理测试数据，替代 @DirtiesContext 避免上下文重建开销。
     * 使用 TRUNCATE TABLE 重置表数据和自增ID，保持基础数据（分类、字典等）不变。
     */
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanupTestData() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // 禁用外键约束以支持任意顺序清理
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            // 清理测试写入的表（使用 @TableName 实际表名，非实体类名）
            stmt.execute("TRUNCATE TABLE robot_param_value");
            stmt.execute("TRUNCATE TABLE robot_video");
            stmt.execute("TRUNCATE TABLE robot_tag");
            stmt.execute("TRUNCATE TABLE robot_price");
            stmt.execute("TRUNCATE TABLE robot_image");
            stmt.execute("TRUNCATE TABLE robot_param_def");
            stmt.execute("TRUNCATE TABLE robot");
            stmt.execute("TRUNCATE TABLE article");
            stmt.execute("TRUNCATE TABLE crawler_media");
            stmt.execute("TRUNCATE TABLE crawler_product");
            stmt.execute("TRUNCATE TABLE crawler_article");
            // 恢复外键约束
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    // ==================== 文章发布集成测试 ====================

    @Test
    @DisplayName("完整文章发布链路：crawler_article -> article + source_url 写入")
    void articlePublishFullChain() {
        CrawlerArticle ca = createCrawlerArticle("测试文章标题", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);
        assertNotNull(ca.getId());

        boolean published = txService.publishSingleArticle(ca, 1L);
        assertTrue(published, "文章应发布成功");

        CrawlerArticle updated = crawlerArticleMapper.selectById(ca.getId());
        assertEquals(1, updated.getSynced(), "synced 应为 1");
        assertEquals("PUBLISHED", updated.getArticleStatus(), "状态应为 PUBLISHED");
        assertNotNull(updated.getArticleId(), "articleId 应已设置");

        PublishArticle article = publishArticleMapper.selectById(updated.getArticleId());
        assertNotNull(article, "article 记录应存在");
        assertEquals("测试文章标题", article.getTitle());
        assertEquals(1L, article.getCategoryId());
        assertEquals("https://example.com/article/1", article.getSourceUrl(), "source_url 应写入");
        assertEquals("测试来源", article.getSource());
    }

    @Test
    @DisplayName("文章CAS幂等：synced=1 时不可重复发布")
    void articlePublishIdempotent() {
        CrawlerArticle ca = createCrawlerArticle("幂等测试", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        boolean first = txService.publishSingleArticle(ca, 1L);
        assertTrue(first);

        CrawlerArticle updated = crawlerArticleMapper.selectById(ca.getId());
        assertEquals(1, updated.getSynced());

        boolean second = txService.publishSingleArticle(updated, 1L);
        assertFalse(second, "已发布的文章不应重复发布");

        long articleCount = publishArticleMapper.selectCount(
                new LambdaQueryWrapper<PublishArticle>().eq(PublishArticle::getTitle, "幂等测试"));
        assertEquals(1, articleCount, "article 表不应有重复记录");
    }

    @Test
    @DisplayName("文章重复发布更新：已有 articleId 时更新而非插入")
    void articleRepublishUpdates() {
        CrawlerArticle ca = createCrawlerArticle("更新测试", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        txService.publishSingleArticle(ca, 1L);
        CrawlerArticle afterFirst = crawlerArticleMapper.selectById(ca.getId());
        Long articleId = afterFirst.getArticleId();

        crawlerArticleMapper.update(null,
                new LambdaUpdateWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getId, ca.getId())
                        .set(CrawlerArticle::getSynced, 0)
                        .set(CrawlerArticle::getArticleStatus, "AUTO_APPROVED")
                        .set(CrawlerArticle::getTitle, "更新后的标题"));

        CrawlerArticle forRepublish = crawlerArticleMapper.selectById(ca.getId());
        boolean republished = txService.publishSingleArticle(forRepublish, 1L);
        assertTrue(republished);

        PublishArticle article = publishArticleMapper.selectById(articleId);
        assertEquals("更新后的标题", article.getTitle(), "标题应被更新");
        assertEquals(articleId, article.getId(), "article ID 应不变");
    }

    @Test
    @DisplayName("文章发布失败后标记 FAILED 并重置 synced 允许重试")
    void articlePublishFailureAllowsRetry() {
        CrawlerArticle ca = createCrawlerArticle("失败测试", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        crawlerArticleMapper.update(null,
                new LambdaUpdateWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getId, ca.getId())
                        .set(CrawlerArticle::getSynced, 1)
                        .set(CrawlerArticle::getArticleStatus, "PUBLISHING"));

        txService.markArticleFailed(ca.getId(), "模拟数据库异常");

        CrawlerArticle failed = crawlerArticleMapper.selectById(ca.getId());
        assertEquals("FAILED", failed.getArticleStatus(), "状态应为 FAILED");
        assertEquals(0, failed.getSynced(), "synced 应重置为 0 允许重试");
    }

    // ==================== 产品发布集成测试 ====================

    @Test
    @DisplayName("完整产品发布链路：crawler_product -> robot + image + price + tag")
    void productPublishFullChain() {
        CrawlerProduct cp = createCrawlerProduct("测试机器人", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        PublishRobotParamDef paramDef = new PublishRobotParamDef();
        paramDef.setGroupId(1L);
        paramDef.setName("重量");
        paramDef.setUnit("kg");
        paramDef.setType("number");
        paramDef.setSort(1);
        paramDef.setIsCompare(1);
        paramDef.setIsShow(1);
        paramDef.setCreateTime(LocalDateTime.now());
        publishRobotParamDefMapper.insert(paramDef);

        boolean published = txService.publishSingleProduct(cp, 1L);
        assertTrue(published, "产品应发布成功");

        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        assertEquals(1, updated.getSynced());
        assertEquals("PUBLISHED", updated.getProductStatus());
        assertNotNull(updated.getRobotIdSynced());

        PublishRobot robot = publishRobotMapper.selectById(updated.getRobotIdSynced());
        assertNotNull(robot);
        assertEquals("测试机器人", robot.getName());

        long imageCount = publishRobotImageMapper.selectCount(
                new LambdaQueryWrapper<PublishRobotImage>().eq(PublishRobotImage::getRobotId, robot.getId()));
        assertTrue(imageCount > 0, "应有图片记录");

        long priceCount = publishRobotPriceMapper.selectCount(
                new LambdaQueryWrapper<PublishRobotPrice>().eq(PublishRobotPrice::getRobotId, robot.getId()));
        assertTrue(priceCount > 0, "应有价格记录");

        long tagCount = publishRobotTagMapper.selectCount(
                new LambdaQueryWrapper<PublishRobotTag>().eq(PublishRobotTag::getRobotId, robot.getId()));
        assertTrue(tagCount > 0, "应有标签记录");
    }

    @Test
    @DisplayName("产品发布CAS幂等：synced=1 时不可重复发布")
    void productPublishIdempotent() {
        CrawlerProduct cp = createCrawlerProduct("幂等产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        boolean first = txService.publishSingleProduct(cp, 1L);
        assertTrue(first);

        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        boolean second = txService.publishSingleProduct(updated, 1L);
        assertFalse(second, "已发布的产品不应重复发布");
    }

    @Test
    @DisplayName("产品重复发布更新：已有 robotIdSynced 时更新关联数据")
    void productRepublishUpdatesRelatedData() {
        CrawlerProduct cp = createCrawlerProduct("更新产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        txService.publishSingleProduct(cp, 1L);
        CrawlerProduct afterFirst = crawlerProductMapper.selectById(cp.getId());
        Long robotId = afterFirst.getRobotIdSynced();

        crawlerProductMapper.update(null,
                new LambdaUpdateWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getId, cp.getId())
                        .set(CrawlerProduct::getSynced, 0)
                        .set(CrawlerProduct::getProductStatus, "AUTO_APPROVED"));

        CrawlerProduct forRepublish = crawlerProductMapper.selectById(cp.getId());
        boolean republished = txService.publishSingleProduct(forRepublish, 1L);
        assertTrue(republished);

        CrawlerProduct afterSecond = crawlerProductMapper.selectById(cp.getId());
        assertEquals(robotId, afterSecond.getRobotIdSynced(), "robot ID 应不变");
    }

    @Test
    @DisplayName("产品发布失败后标记 FAILED 并重置 synced")
    void productPublishFailureAllowsRetry() {
        CrawlerProduct cp = createCrawlerProduct("失败产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        txService.markProductFailed(cp.getId(), "模拟数据库异常");

        CrawlerProduct failed = crawlerProductMapper.selectById(cp.getId());
        assertEquals("FAILED", failed.getProductStatus());
        assertEquals(0, failed.getSynced(), "synced 应重置为 0 允许重试");
    }

    // ==================== source_url 集成测试 ====================

    @Test
    @DisplayName("source_url 在真实数据库中成功写入和读取")
    void sourceUrlWriteAndRead() {
        CrawlerArticle ca = createCrawlerArticle("source_url测试", "AUTO_APPROVED", 0);
        ca.setSourceUrl("https://mp.weixin.qq.com/s/test-article-123");
        crawlerArticleMapper.insert(ca);

        txService.publishSingleArticle(ca, 1L);

        CrawlerArticle updated = crawlerArticleMapper.selectById(ca.getId());
        PublishArticle article = publishArticleMapper.selectById(updated.getArticleId());

        assertNotNull(article.getSourceUrl(), "sourceUrl 不应为 null");
        assertEquals("https://mp.weixin.qq.com/s/test-article-123", article.getSourceUrl(),
                "sourceUrl 应与原始值一致");
    }

    // ==================== 超时恢复测试 ====================

    @Test
    @DisplayName("PUBLISHING 超时恢复：超时记录被重置为 FAILED")
    void publishingTimeoutRecovery() {
        CrawlerArticle ca = createCrawlerArticle("超时文章", "PUBLISHING", 1);
        ca.setUpdateTime(LocalDateTime.now().minusMinutes(60));
        crawlerArticleMapper.insert(ca);

        int recovered = txService.recoverTimedOutPublishing(30);
        assertTrue(recovered > 0, "应恢复超时记录");

        CrawlerArticle recoveredArticle = crawlerArticleMapper.selectById(ca.getId());
        assertEquals("FAILED", recoveredArticle.getArticleStatus(), "状态应为 FAILED");
        assertEquals(0, recoveredArticle.getSynced(), "synced 应重置为 0");
    }

    // ==================== 失败原因和重试次数持久化测试 ====================

    @Test
    @DisplayName("markArticleFailed持久化失败原因和重试次数")
    void markArticleFailedPersistsReasonAndRetryCount() {
        CrawlerArticle ca = createCrawlerArticle("失败原因测试", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        txService.markArticleFailed(ca.getId(), "数据库连接超时");

        CrawlerArticle failed1 = crawlerArticleMapper.selectById(ca.getId());
        assertEquals("FAILED", failed1.getArticleStatus());
        assertEquals(0, failed1.getSynced(), "synced 应重置为 0 允许重试");
        assertEquals("数据库连接超时", failed1.getFailReason(), "失败原因应持久化");
        assertEquals(1, failed1.getRetryCount(), "重试次数应为 1");

        txService.markArticleFailed(ca.getId(), "主键冲突");

        CrawlerArticle failed2 = crawlerArticleMapper.selectById(ca.getId());
        assertEquals(2, failed2.getRetryCount(), "重试次数应递增为 2");
        assertEquals("主键冲突", failed2.getFailReason(), "失败原因应更新");
    }

    @Test
    @DisplayName("markProductFailed持久化失败原因和重试次数")
    void markProductFailedPersistsReasonAndRetryCount() {
        CrawlerProduct cp = createCrawlerProduct("失败原因产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        txService.markProductFailed(cp.getId(), "JSON解析失败");

        CrawlerProduct failed = crawlerProductMapper.selectById(cp.getId());
        assertEquals("FAILED", failed.getProductStatus());
        assertEquals("JSON解析失败", failed.getFailReason(), "失败原因应持久化");
        assertEquals(1, failed.getRetryCount(), "重试次数应为 1");
    }

    @Test
    @DisplayName("markProductPendingReview标记待审核状态")
    void markProductPendingReviewStatus() {
        CrawlerProduct cp = createCrawlerProduct("待审核产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        txService.markProductPendingReview(cp.getId(), "图集JSON格式错误");

        CrawlerProduct review = crawlerProductMapper.selectById(cp.getId());
        assertEquals("PENDING_REVIEW", review.getProductStatus(), "状态应为 PENDING_REVIEW");
        assertEquals(0, review.getSynced(), "synced 应为 0");
        assertEquals("图集JSON格式错误", review.getFailReason(), "原因应持久化");
    }

    // ==================== 事务回滚测试 ====================

    @Test
    @DisplayName("产品发布：无效价格仅警告不回滚主表")
    void productPublishInvalidPriceWarningOnly() {
        CrawlerProduct cp = createCrawlerProduct("回滚测试产品", "AUTO_APPROVED", 0);
        cp.setPrice("invalid-price-not-a-number");
        crawlerProductMapper.insert(cp);

        boolean published = txService.publishSingleProduct(cp, 1L);
        assertTrue(published, "产品应发布成功（价格警告不回滚主表）");

        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        assertEquals("PUBLISHED", updated.getProductStatus(), "产品应已发布");
        assertNotNull(updated.getRobotIdSynced(), "robot ID 应已设置");

        PublishRobot robot = publishRobotMapper.selectById(updated.getRobotIdSynced());
        assertNotNull(robot, "robot 记录应存在");
    }

    // ==================== 并发测试 ====================

    @Test
    @DisplayName("并发发布同一文章：CAS幂等保证只有一次成功")
    void concurrentArticlePublishIdempotent() throws Exception {
        CrawlerArticle ca = createCrawlerArticle("并发测试文章", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        CrawlerArticle fresh = crawlerArticleMapper.selectById(ca.getId());
                        boolean result = txService.publishSingleArticle(fresh, 1L);
                        if (result) successCount.incrementAndGet();
                    } catch (Exception e) {
                        // CAS失败是预期行为，记录但不中断
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "并发测试应在10秒内完成");

            assertEquals(1, successCount.get(), "并发发布同一文章，只有1个线程应成功");

            long articleCount = publishArticleMapper.selectCount(
                    new LambdaQueryWrapper<PublishArticle>().eq(PublishArticle::getTitle, "并发测试文章"));
            assertEquals(1, articleCount, "article 表不应有重复记录");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("并发发布同一产品：CAS幂等保证只有一次成功")
    void concurrentProductPublishIdempotent() throws Exception {
        CrawlerProduct cp = createCrawlerProduct("并发测试产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        CrawlerProduct fresh = crawlerProductMapper.selectById(cp.getId());
                        boolean result = txService.publishSingleProduct(fresh, 1L);
                        if (result) successCount.incrementAndGet();
                    } catch (Exception e) {
                        // CAS失败是预期行为，记录但不中断
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "并发测试应在10秒内完成");

            assertEquals(1, successCount.get(), "并发发布同一产品，只有1个线程应成功");
        } finally {
            executor.shutdownNow();
        }
    }

    // ==================== 最大重试次数测试 ====================

    @Test
    @DisplayName("文章超过最大重试次数后标记为 MANUAL_REVIEW")
    void articleMaxRetryExceededMarksManualReview() {
        CrawlerArticle ca = createCrawlerArticle("重试超限文章", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        // 模拟多次失败重试
        for (int i = 1; i <= 3; i++) {
            txService.markArticleFailed(ca.getId(), "模拟失败第" + i + "次");
            CrawlerArticle after = crawlerArticleMapper.selectById(ca.getId());
            assertEquals("FAILED", after.getArticleStatus(), "第" + i + "次失败应为 FAILED");
            assertEquals(i, after.getRetryCount(), "重试次数应为 " + i);
        }

        // 第4次失败应标记为 MANUAL_REVIEW
        txService.markArticleFailed(ca.getId(), "模拟失败第4次");
        CrawlerArticle exceeded = crawlerArticleMapper.selectById(ca.getId());
        assertEquals("MANUAL_REVIEW", exceeded.getArticleStatus(), "超过最大重试次数应标记为 MANUAL_REVIEW");
        assertEquals(0, exceeded.getSynced(), "synced 应为 0");
        assertTrue(exceeded.getFailReason().contains("需人工介入"), "失败原因应包含需人工介入");
    }

    @Test
    @DisplayName("产品超过最大重试次数后标记为 MANUAL_REVIEW")
    void productMaxRetryExceededMarksManualReview() {
        CrawlerProduct cp = createCrawlerProduct("重试超限产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        // 模拟多次失败重试
        for (int i = 1; i <= 3; i++) {
            txService.markProductFailed(cp.getId(), "模拟失败第" + i + "次");
            CrawlerProduct after = crawlerProductMapper.selectById(cp.getId());
            assertEquals("FAILED", after.getProductStatus(), "第" + i + "次失败应为 FAILED");
            assertEquals(i, after.getRetryCount(), "重试次数应为 " + i);
        }

        // 第4次失败应标记为 MANUAL_REVIEW
        txService.markProductFailed(cp.getId(), "模拟失败第4次");
        CrawlerProduct exceeded = crawlerProductMapper.selectById(cp.getId());
        assertEquals("MANUAL_REVIEW", exceeded.getProductStatus(), "超过最大重试次数应标记为 MANUAL_REVIEW");
        assertEquals(0, exceeded.getSynced(), "synced 应为 0");
        assertTrue(exceeded.getFailReason().contains("需人工介入"), "失败原因应包含需人工介入");
    }

    // ==================== 事务回滚验证测试 ====================

    @Test
    @DisplayName("真实故障注入：robot插入失败时事务完整回滚")
    void productPublishDbErrorRollsBackEntireTransaction() {
        // 故障注入：创建一个产品名超过robot.name列长度限制(VARCHAR(128))的产品
        // crawler_product.product_name是VARCHAR(255)可以存储，但robot.name是VARCHAR(128)会失败
        String longName = String.format("%200s", "").replace(' ', 'A'); // 200字符，超过robot.name的128限制
        CrawlerProduct cp = createCrawlerProduct(longName, "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        // 调用发布方法应抛出异常（robot.name列长度溢出触发DataIntegrityViolation）
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> txService.publishSingleProduct(cp, 1L),
                "robot插入失败应抛出异常触发事务回滚");
        assertTrue(ex.getMessage().contains("Product publish failed"),
                "异常信息应包含发布失败描述");

        // 验证事务完整回滚：robot表中不存在该产品记录
        long robotCount = publishRobotMapper.selectCount(
                new LambdaQueryWrapper<PublishRobot>().eq(PublishRobot::getName, longName));
        assertEquals(0, robotCount, "事务回滚后robot表中不应存在记录");

        // 验证关联数据也被回滚：无图片、价格、标签记录
        long imageCount = publishRobotImageMapper.selectCount(new LambdaQueryWrapper<>());
        long priceCount = publishRobotPriceMapper.selectCount(new LambdaQueryWrapper<>());
        long tagCount = publishRobotTagMapper.selectCount(new LambdaQueryWrapper<>());
        assertEquals(0, imageCount, "事务回滚后不应有图片记录");
        assertEquals(0, priceCount, "事务回滚后不应有价格记录");
        assertEquals(0, tagCount, "事务回滚后不应有标签记录");

        // 验证CAS更新也被回滚：crawler_product恢复为synced=0
        CrawlerProduct afterRollback = crawlerProductMapper.selectById(cp.getId());
        assertEquals(0, afterRollback.getSynced(), "CAS更新应随事务一起回滚，synced恢复为0");
    }

    @Test
    @DisplayName("无效gallery JSON标记PENDING_REVIEW且不创建robot记录")
    void invalidGalleryMarksPendingReviewWithoutCreatingRobot() {
        CrawlerProduct cp = createCrawlerProduct("无效图集产品", "AUTO_APPROVED", 0);
        cp.setGallery("not-a-valid-json-array");
        crawlerProductMapper.insert(cp);

        boolean handled = txService.publishSingleProduct(cp, 1L);
        assertTrue(handled, "产品应处理成功（gallery JSON错误标记PENDING_REVIEW）");

        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        assertEquals("PENDING_REVIEW", updated.getProductStatus(), "无效gallery JSON应标记PENDING_REVIEW");
        assertEquals(0, updated.getSynced(), "PENDING_REVIEW synced应为0");
        assertNull(updated.getRobotIdSynced(), "PENDING_REVIEW时不应设置robotIdSynced");

        // 验证正式robot表中不存在该产品的记录
        long robotCount = publishRobotMapper.selectCount(
                new LambdaQueryWrapper<PublishRobot>().eq(PublishRobot::getName, "无效图集产品"));
        assertEquals(0, robotCount, "PENDING_REVIEW产品不应在正式robot表中创建记录");
    }

    // ==================== 完整发布链路测试 ====================

    @Test
    @DisplayName("完整产品发布链路：包含参数")
    void productPublishFullChainWithParams() {
        CrawlerProduct cp = createCrawlerProduct("完整链路产品", "AUTO_APPROVED", 0);
        cp.setNormalizedParams("{\"重量\":\"25kg\",\"负载\":\"5kg\"}");
        crawlerProductMapper.insert(cp);

        PublishRobotParamDef weightDef = new PublishRobotParamDef();
        weightDef.setGroupId(1L);
        weightDef.setName("重量");
        weightDef.setUnit("kg");
        weightDef.setType("number");
        weightDef.setSort(1);
        weightDef.setIsCompare(1);
        weightDef.setIsShow(1);
        weightDef.setCreateTime(LocalDateTime.now());
        publishRobotParamDefMapper.insert(weightDef);

        PublishRobotParamDef loadDef = new PublishRobotParamDef();
        loadDef.setGroupId(1L);
        loadDef.setName("负载");
        loadDef.setUnit("kg");
        loadDef.setType("number");
        loadDef.setSort(2);
        loadDef.setIsCompare(1);
        loadDef.setIsShow(1);
        loadDef.setCreateTime(LocalDateTime.now());
        publishRobotParamDefMapper.insert(loadDef);

        boolean published = txService.publishSingleProduct(cp, 1L);
        assertTrue(published, "产品应发布成功");

        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        PublishRobot robot = publishRobotMapper.selectById(updated.getRobotIdSynced());

        long paramCount = publishRobotParamValueMapper.selectCount(
                new LambdaQueryWrapper<PublishRobotParamValue>().eq(PublishRobotParamValue::getRobotId, robot.getId()));
        assertEquals(2, paramCount, "应有2个参数值");
    }

    @Test
    @DisplayName("无效normalizedParams JSON标记PENDING_REVIEW且不创建robot记录")
    void invalidNormalizedParamsMarksPendingReviewWithoutCreatingRobot() {
        CrawlerProduct cp = createCrawlerProduct("无效参数产品", "AUTO_APPROVED", 0);
        cp.setNormalizedParams("not-a-valid-json-object");
        crawlerProductMapper.insert(cp);

        boolean handled = txService.publishSingleProduct(cp, 1L);
        assertTrue(handled, "产品应处理成功（normalizedParams JSON错误标记PENDING_REVIEW）");

        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        assertEquals("PENDING_REVIEW", updated.getProductStatus(), "无效normalizedParams JSON应标记PENDING_REVIEW");
        assertEquals(0, updated.getSynced(), "PENDING_REVIEW synced应为0");
        assertNull(updated.getRobotIdSynced(), "PENDING_REVIEW时不应设置robotIdSynced");

        long robotCount = publishRobotMapper.selectCount(
                new LambdaQueryWrapper<PublishRobot>().eq(PublishRobot::getName, "无效参数产品"));
        assertEquals(0, robotCount, "PENDING_REVIEW产品不应在正式robot表中创建记录");
    }

    @Test
    @DisplayName("gallery和normalizedParams均无效时标记PENDING_REVIEW且不创建robot记录")
    void bothInvalidJsonMarksPendingReviewWithoutCreatingRobot() {
        CrawlerProduct cp = createCrawlerProduct("双重无效产品", "AUTO_APPROVED", 0);
        cp.setGallery("not-a-valid-json-array");
        cp.setNormalizedParams("not-a-valid-json-object");
        crawlerProductMapper.insert(cp);

        boolean handled = txService.publishSingleProduct(cp, 1L);
        assertTrue(handled, "产品应处理成功");

        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        assertEquals("PENDING_REVIEW", updated.getProductStatus(), "双重无效JSON应标记PENDING_REVIEW");
        assertEquals(0, updated.getSynced(), "PENDING_REVIEW synced应为0");
        assertNull(updated.getRobotIdSynced(), "PENDING_REVIEW时不应设置robotIdSynced");
        assertNotNull(updated.getFailReason(), "失败原因应持久化");
        assertTrue(updated.getFailReason().contains("gallery") || updated.getFailReason().contains("normalizedParams"),
                "失败原因应包含具体JSON错误信息");
    }

    @Test
    @DisplayName("有效gallery和normalizedParams通过预校验后正常发布")
    void validJsonPassesPreValidationAndPublishes() {
        CrawlerProduct cp = createCrawlerProduct("有效数据产品", "AUTO_APPROVED", 0);
        cp.setGallery("[\"https://example.com/img1.jpg\",\"https://example.com/img2.jpg\"]");
        cp.setNormalizedParams("{\"重量\":\"25kg\",\"负载\":\"5kg\"}");
        crawlerProductMapper.insert(cp);

        PublishRobotParamDef weightDef = new PublishRobotParamDef();
        weightDef.setGroupId(1L);
        weightDef.setName("重量");
        weightDef.setUnit("kg");
        weightDef.setType("number");
        weightDef.setSort(1);
        weightDef.setIsCompare(1);
        weightDef.setIsShow(1);
        weightDef.setCreateTime(LocalDateTime.now());
        publishRobotParamDefMapper.insert(weightDef);

        boolean published = txService.publishSingleProduct(cp, 1L);
        assertTrue(published, "有效数据应发布成功");

        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        assertEquals("PUBLISHED", updated.getProductStatus(), "有效数据应标记为PUBLISHED");
        assertNotNull(updated.getRobotIdSynced(), "PUBLISHED时robotIdSynced应已设置");

        PublishRobot robot = publishRobotMapper.selectById(updated.getRobotIdSynced());
        assertNotNull(robot, "robot记录应存在");

        long imageCount = publishRobotImageMapper.selectCount(
                new LambdaQueryWrapper<PublishRobotImage>().eq(PublishRobotImage::getRobotId, robot.getId()));
        assertTrue(imageCount >= 2, "应有gallery图片记录");

        long paramCount = publishRobotParamValueMapper.selectCount(
                new LambdaQueryWrapper<PublishRobotParamValue>().eq(PublishRobotParamValue::getRobotId, robot.getId()));
        assertEquals(1, paramCount, "应有1个参数值（重量）");
    }

    // ==================== 指数退避重试测试 ====================

    @Test
    @DisplayName("文章发布失败设置指数退避next_retry_time")
    void articleFailedSetsExponentialBackoffNextRetryTime() {
        CrawlerArticle ca = createCrawlerArticle("退避测试文章", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        // 第一次失败：retry_count=1, next_retry_time = NOW() + 10s * 2^0 = NOW() + 10s
        txService.markArticleFailed(ca.getId(), "测试失败原因");

        CrawlerArticle afterFirst = crawlerArticleMapper.selectById(ca.getId());
        assertEquals("FAILED", afterFirst.getArticleStatus());
        assertEquals(1, afterFirst.getRetryCount(), "第一次失败retry_count应为1");
        assertNotNull(afterFirst.getNextRetryTime(), "next_retry_time应已设置");
        // 验证退避间隔：应在 NOW()+8s 到 NOW()+15s 之间（10s基础间隔±容差）
        LocalDateTime expectedFirst = LocalDateTime.now().plusSeconds(10);
        assertTrue(afterFirst.getNextRetryTime().isAfter(LocalDateTime.now().plusSeconds(5)),
                "第一次重试时间应至少5秒后");
        assertTrue(afterFirst.getNextRetryTime().isBefore(LocalDateTime.now().plusSeconds(20)),
                "第一次重试时间应在20秒内");

        // 第二次失败：retry_count=2, next_retry_time = NOW() + 10s * 2^1 = NOW() + 20s
        txService.markArticleFailed(ca.getId(), "再次失败");

        CrawlerArticle afterSecond = crawlerArticleMapper.selectById(ca.getId());
        assertEquals(2, afterSecond.getRetryCount(), "第二次失败retry_count应为2");
        assertTrue(afterSecond.getNextRetryTime().isAfter(afterFirst.getNextRetryTime()),
                "第二次重试时间应晚于第一次");

        // 第三次失败：retry_count=3, next_retry_time = NOW() + 10s * 2^2 = NOW() + 40s
        txService.markArticleFailed(ca.getId(), "第三次失败");

        CrawlerArticle afterThird = crawlerArticleMapper.selectById(ca.getId());
        assertEquals(3, afterThird.getRetryCount(), "第三次失败retry_count应为3");
        assertEquals("FAILED", afterThird.getArticleStatus(),
                "retry_count=3未超过max-retries(3)，仍为FAILED");
        assertTrue(afterThird.getNextRetryTime().isAfter(afterSecond.getNextRetryTime()),
                "第三次重试时间应晚于第二次");

        // 第四次失败：retry_count=4 > max-retries(3)，标记为MANUAL_REVIEW
        txService.markArticleFailed(ca.getId(), "第四次失败");

        CrawlerArticle afterFourth = crawlerArticleMapper.selectById(ca.getId());
        assertEquals("MANUAL_REVIEW", afterFourth.getArticleStatus(),
                "超过max-retries(3)应标记为MANUAL_REVIEW");
        assertTrue(afterFourth.getFailReason().contains("需人工介入"),
                "超过重试次数的fail_reason应包含需人工介入");
    }

    @Test
    @DisplayName("产品发布失败设置指数退避next_retry_time")
    void productFailedSetsExponentialBackoffNextRetryTime() {
        CrawlerProduct cp = createCrawlerProduct("退避测试产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        // 第一次失败：retry_count=1, next_retry_time = NOW() + 10s
        txService.markProductFailed(cp.getId(), "测试失败原因");

        CrawlerProduct afterFirst = crawlerProductMapper.selectById(cp.getId());
        assertEquals("FAILED", afterFirst.getProductStatus());
        assertEquals(1, afterFirst.getRetryCount(), "第一次失败retry_count应为1");
        assertNotNull(afterFirst.getNextRetryTime(), "next_retry_time应已设置");

        // 第二次失败：retry_count=2, next_retry_time = NOW() + 20s
        txService.markProductFailed(cp.getId(), "再次失败");

        CrawlerProduct afterSecond = crawlerProductMapper.selectById(cp.getId());
        assertEquals(2, afterSecond.getRetryCount(), "第二次失败retry_count应为2");
        assertTrue(afterSecond.getNextRetryTime().isAfter(afterFirst.getNextRetryTime()),
                "第二次重试时间应晚于第一次");
    }

    @Test
    @DisplayName("超时恢复记录失败原因和重试次数")
    void recoverTimedOutSetsFailReasonAndRetryCount() {
        // 创建一个PUBLISHING状态且update_time在阈值之前的产品
        CrawlerProduct cp = createCrawlerProduct("超时测试产品", "PUBLISHING", 1);
        cp.setUpdateTime(LocalDateTime.now().minusMinutes(60)); // 60分钟前
        crawlerProductMapper.insert(cp);

        int recovered = txService.recoverTimedOutPublishing(30); // 30分钟超时

        assertEquals(1, recovered, "应恢复1条超时记录");
        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        assertEquals("FAILED", updated.getProductStatus(), "超时记录应标记为FAILED");
        assertEquals(0, updated.getSynced(), "synced应重置为0");
        assertEquals(1, updated.getRetryCount(), "retry_count应增加1");
        assertNotNull(updated.getFailReason(), "fail_reason应已设置");
        assertTrue(updated.getFailReason().contains("超时"), "fail_reason应包含超时信息");
        assertNotNull(updated.getNextRetryTime(), "next_retry_time应已设置（指数退避）");
    }

    // ==================== 辅助方法 ====================

    private CrawlerArticle createCrawlerArticle(String title, String status, int synced) {
        CrawlerArticle ca = new CrawlerArticle();
        ca.setSourceId(1L);
        ca.setSourceUrl("https://example.com/article/1");
        ca.setSourceName("测试来源");
        ca.setSourceSite("example.com");
        ca.setTitle(title);
        ca.setSummary("测试摘要");
        ca.setContentHtml("<p>测试内容</p>");
        ca.setAuthor("测试作者");
        ca.setMatchStatus("MATCHED");
        ca.setArticleStatus(status);
        ca.setSynced(synced);
        ca.setCreateTime(LocalDateTime.now());
        ca.setUpdateTime(LocalDateTime.now());
        return ca;
    }

    private CrawlerProduct createCrawlerProduct(String name, String status, int synced) {
        CrawlerProduct cp = new CrawlerProduct();
        cp.setSourceId(1L);
        cp.setSourceUrl("https://example.com/product/1");
        cp.setProductName(name);
        cp.setCategory("人形机器人");
        cp.setSummary("测试摘要");
        cp.setCoverImage("https://example.com/cover.jpg");
        cp.setGallery("[\"https://example.com/img1.jpg\",\"https://example.com/img2.jpg\"]");
        cp.setPrice("99999");
        cp.setMatchStatus("MATCHED");
        cp.setProductStatus(status);
        cp.setSynced(synced);
        cp.setCreateTime(LocalDateTime.now());
        cp.setUpdateTime(LocalDateTime.now());
        return cp;
    }
}