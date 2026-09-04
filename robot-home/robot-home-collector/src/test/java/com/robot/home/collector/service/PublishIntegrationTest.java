package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.robot.home.collector.CollectorApplication;
import com.robot.home.collector.CollectorTestDataSourceInitializer;
import com.robot.home.collector.entity.*;
import com.robot.home.collector.mapper.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 发布服务数据库集成测试
 * 使用 @SpringBootTest + H2 内存数据库，验证真实 SQL 执行和事务行为。
 *
 * ⚠️ 与 Mockito 单元测试的区别：
 * - Mockito 测试只验证调用次数，不验证 SQL 正确性和事务回滚
 * - 本测试验证完整数据库链路：INSERT/UPDATE/DELETE + 事务提交/回滚
 * - 移除事务修复后本测试必须失败，否则说明测试无效
 */
@SpringBootTest(
        classes = {CollectorApplication.class, CollectorTestDataSourceInitializer.class, PublishIntegrationTest.TestConfig.class},
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("发布服务数据库集成测试")
public class PublishIntegrationTest {

    /** 测试配置：提供 Mock Bean 替代 Redis 和 MySQL 特有组件 */
    @TestConfiguration
    static class TestConfig {
        /** Mock Redis 连接工厂，避免测试环境连接真实 Redis */
        @Bean
        @Primary
        public RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

        /** Mock Redis，避免测试环境连接真实 Redis */
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

    // ==================== 文章发布集成测试 ====================

    @Test
    @DisplayName("完整文章发布链路：crawler_article → article + source_url 写入")
    @Transactional
    void articlePublishFullChain() {
        // 1. 插入待发布的采集文章
        CrawlerArticle ca = createCrawlerArticle("测试文章标题", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);
        assertNotNull(ca.getId());

        // 2. 执行发布
        boolean published = txService.publishSingleArticle(ca, 1L);
        assertTrue(published, "文章应发布成功");

        // 3. 验证采集状态更新
        CrawlerArticle updated = crawlerArticleMapper.selectById(ca.getId());
        assertEquals(1, updated.getSynced(), "synced 应为 1");
        assertEquals("PUBLISHED", updated.getArticleStatus(), "状态应为 PUBLISHED");
        assertNotNull(updated.getArticleId(), "articleId 应已设置");

        // 4. 验证 article 表记录
        PublishArticle article = publishArticleMapper.selectById(updated.getArticleId());
        assertNotNull(article, "article 记录应存在");
        assertEquals("测试文章标题", article.getTitle());
        assertEquals(1L, article.getCategoryId());
        assertEquals("https://example.com/article/1", article.getSourceUrl(), "source_url 应写入");
        assertEquals("测试来源", article.getSource());
    }

    @Test
    @DisplayName("文章CAS幂等：synced=1 时不可重复发布")
    @Transactional
    void articlePublishIdempotent() {
        CrawlerArticle ca = createCrawlerArticle("幂等测试", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        // 第一次发布成功
        boolean first = txService.publishSingleArticle(ca, 1L);
        assertTrue(first);

        // 重新查询已更新的记录
        CrawlerArticle updated = crawlerArticleMapper.selectById(ca.getId());
        assertEquals(1, updated.getSynced());

        // 第二次发布应返回 false（CAS 失败）
        boolean second = txService.publishSingleArticle(updated, 1L);
        assertFalse(second, "已发布的文章不应重复发布");

        // article 表应只有一条记录
        long articleCount = publishArticleMapper.selectCount(
                new LambdaQueryWrapper<PublishArticle>().eq(PublishArticle::getTitle, "幂等测试"));
        assertEquals(1, articleCount, "article 表不应有重复记录");
    }

    @Test
    @DisplayName("文章重复发布更新：已有 articleId 时更新而非插入")
    @Transactional
    void articleRepublishUpdates() {
        CrawlerArticle ca = createCrawlerArticle("更新测试", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        // 第一次发布
        txService.publishSingleArticle(ca, 1L);
        CrawlerArticle afterFirst = crawlerArticleMapper.selectById(ca.getId());
        Long articleId = afterFirst.getArticleId();

        // 模拟重新采集：重置 synced 并修改标题
        crawlerArticleMapper.update(null,
                new LambdaUpdateWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getId, ca.getId())
                        .set(CrawlerArticle::getSynced, 0)
                        .set(CrawlerArticle::getArticleStatus, "AUTO_APPROVED")
                        .set(CrawlerArticle::getTitle, "更新后的标题"));

        // 第二次发布（应更新而非插入）
        CrawlerArticle forRepublish = crawlerArticleMapper.selectById(ca.getId());
        boolean republished = txService.publishSingleArticle(forRepublish, 1L);
        assertTrue(republished);

        // 验证 article 表记录被更新
        PublishArticle article = publishArticleMapper.selectById(articleId);
        assertEquals("更新后的标题", article.getTitle(), "标题应被更新");
        assertEquals(articleId, article.getId(), "article ID 应不变");
    }

    @Test
    @DisplayName("文章发布失败后标记 FAILED 并重置 synced 允许重试")
    @Transactional
    void articlePublishFailureAllowsRetry() {
        CrawlerArticle ca = createCrawlerArticle("失败测试", "AUTO_APPROVED", 0);
        crawlerArticleMapper.insert(ca);

        // 先 CAS 锁定
        crawlerArticleMapper.update(null,
                new LambdaUpdateWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getId, ca.getId())
                        .set(CrawlerArticle::getSynced, 1)
                        .set(CrawlerArticle::getArticleStatus, "PUBLISHING"));

        // 模拟发布失败后标记
        txService.markArticleFailed(ca.getId(), "模拟数据库异常");

        // 验证状态
        CrawlerArticle failed = crawlerArticleMapper.selectById(ca.getId());
        assertEquals("FAILED", failed.getArticleStatus(), "状态应为 FAILED");
        assertEquals(0, failed.getSynced(), "synced 应重置为 0 允许重试");
    }

    // ==================== 产品发布集成测试 ====================

    @Test
    @DisplayName("完整产品发布链路：crawler_product → robot + image + price + tag")
    @Transactional
    void productPublishFullChain() {
        // 1. 插入待发布的采集产品
        CrawlerProduct cp = createCrawlerProduct("测试机器人", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        // 2. 插入参数定义
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

        // 3. 执行发布
        boolean published = txService.publishSingleProduct(cp, 1L);
        assertTrue(published, "产品应发布成功");

        // 4. 验证采集状态
        CrawlerProduct updated = crawlerProductMapper.selectById(cp.getId());
        assertEquals(1, updated.getSynced());
        assertEquals("PUBLISHED", updated.getProductStatus());
        assertNotNull(updated.getRobotIdSynced());

        // 5. 验证 robot 表
        PublishRobot robot = publishRobotMapper.selectById(updated.getRobotIdSynced());
        assertNotNull(robot);
        assertEquals("测试机器人", robot.getName());

        // 6. 验证关联表
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
    @Transactional
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
    @Transactional
    void productRepublishUpdatesRelatedData() {
        CrawlerProduct cp = createCrawlerProduct("更新产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        // 第一次发布
        txService.publishSingleProduct(cp, 1L);
        CrawlerProduct afterFirst = crawlerProductMapper.selectById(cp.getId());
        Long robotId = afterFirst.getRobotIdSynced();

        // 重置 synced 允许重新发布
        crawlerProductMapper.update(null,
                new LambdaUpdateWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getId, cp.getId())
                        .set(CrawlerProduct::getSynced, 0)
                        .set(CrawlerProduct::getProductStatus, "AUTO_APPROVED"));

        // 第二次发布
        CrawlerProduct forRepublish = crawlerProductMapper.selectById(cp.getId());
        boolean republished = txService.publishSingleProduct(forRepublish, 1L);
        assertTrue(republished);

        // 验证 robot ID 不变
        CrawlerProduct afterSecond = crawlerProductMapper.selectById(cp.getId());
        assertEquals(robotId, afterSecond.getRobotIdSynced(), "robot ID 应不变");
    }

    @Test
    @DisplayName("产品发布失败后标记 FAILED 并重置 synced")
    @Transactional
    void productPublishFailureAllowsRetry() {
        CrawlerProduct cp = createCrawlerProduct("失败产品", "AUTO_APPROVED", 0);
        crawlerProductMapper.insert(cp);

        // 模拟失败
        txService.markProductFailed(cp.getId(), "模拟数据库异常");

        CrawlerProduct failed = crawlerProductMapper.selectById(cp.getId());
        assertEquals("FAILED", failed.getProductStatus());
        assertEquals(0, failed.getSynced(), "synced 应重置为 0 允许重试");
    }

    // ==================== source_url 集成测试 ====================

    @Test
    @DisplayName("source_url 在真实数据库中成功写入和读取")
    @Transactional
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
    @Transactional
    void publishingTimeoutRecovery() {
        // 插入一条 PUBLISHING 状态的记录（模拟崩溃后残留）
        CrawlerArticle ca = createCrawlerArticle("超时文章", "PUBLISHING", 1);
        ca.setUpdateTime(LocalDateTime.now().minusMinutes(60)); // 60分钟前
        crawlerArticleMapper.insert(ca);

        // 执行超时恢复（阈值30分钟）
        int recovered = txService.recoverTimedOutPublishing(30);
        assertTrue(recovered > 0, "应恢复超时记录");

        CrawlerArticle recoveredArticle = crawlerArticleMapper.selectById(ca.getId());
        assertEquals("FAILED", recoveredArticle.getArticleStatus(), "状态应为 FAILED");
        assertEquals(0, recoveredArticle.getSynced(), "synced 应重置为 0");
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