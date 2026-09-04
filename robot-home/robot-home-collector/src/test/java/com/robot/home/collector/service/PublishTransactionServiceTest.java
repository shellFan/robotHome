package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.robot.home.collector.entity.*;
import com.robot.home.collector.mapper.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PublishTransactionService 单元测试（Mockito）
 * 测试事务层核心逻辑：CAS 幂等、新建/更新、关联数据、失败标记
 *
 * ⚠️ 本测试不验证事务回滚行为，那由 PublishIntegrationTest 负责
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("发布事务服务测试")
public class PublishTransactionServiceTest {

    @Mock private CrawlerArticleMapper crawlerArticleMapper;
    @Mock private CrawlerProductMapper crawlerProductMapper;
    @Mock private CrawlerMediaMapper crawlerMediaMapper;
    @Mock private PublishArticleMapper publishArticleMapper;
    @Mock private PublishRobotMapper publishRobotMapper;
    @Mock private PublishRobotImageMapper publishRobotImageMapper;
    @Mock private PublishRobotPriceMapper publishRobotPriceMapper;
    @Mock private PublishRobotTagMapper publishRobotTagMapper;
    @Mock private PublishRobotVideoMapper publishRobotVideoMapper;
    @Mock private PublishRobotParamValueMapper publishRobotParamValueMapper;
    @Mock private PublishRobotParamDefMapper publishRobotParamDefMapper;

    @InjectMocks
    private PublishTransactionService txService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, CrawlerArticle.class);
        TableInfoHelper.initTableInfo(assistant, CrawlerProduct.class);
        TableInfoHelper.initTableInfo(assistant, PublishArticle.class);
        TableInfoHelper.initTableInfo(assistant, PublishRobot.class);
        TableInfoHelper.initTableInfo(assistant, PublishRobotImage.class);
        TableInfoHelper.initTableInfo(assistant, PublishRobotPrice.class);
        TableInfoHelper.initTableInfo(assistant, PublishRobotTag.class);
        TableInfoHelper.initTableInfo(assistant, PublishRobotVideo.class);
        TableInfoHelper.initTableInfo(assistant, PublishRobotParamValue.class);
        TableInfoHelper.initTableInfo(assistant, PublishRobotParamDef.class);
        TableInfoHelper.initTableInfo(assistant, CrawlerMedia.class);
    }

    // ==================== 文章发布事务测试 ====================

    @Nested
    @DisplayName("文章发布事务")
    class ArticleTransactionTests {

        @Test
        @DisplayName("CAS成功-新建article")
        void publishArticle_casSuccess_newInsert() {
            CrawlerArticle ca = createCrawlerArticle(1L, "测试文章", "AUTO_APPROVED", 0);
            ca.setArticleId(null);

            when(crawlerArticleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(publishArticleMapper.insert(any(PublishArticle.class))).thenAnswer(inv -> {
                PublishArticle a = inv.getArgument(0);
                a.setId(100L);
                return 1;
            });

            boolean result = txService.publishSingleArticle(ca, 1L);
            assertTrue(result);
            verify(publishArticleMapper).insert(any(PublishArticle.class));
            // CAS更新 + 标记PUBLISHED = 2次update
            verify(crawlerArticleMapper, times(2)).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("CAS失败-已被其他线程发布")
        void publishArticle_casFailed() {
            CrawlerArticle ca = createCrawlerArticle(1L, "测试文章", "AUTO_APPROVED", 0);
            when(crawlerArticleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

            boolean result = txService.publishSingleArticle(ca, 1L);
            assertFalse(result);
            verify(publishArticleMapper, never()).insert(any());
        }

        @Test
        @DisplayName("已有articleId-更新而非插入")
        void publishArticle_republishUpdates() {
            CrawlerArticle ca = createCrawlerArticle(1L, "测试文章", "AUTO_APPROVED", 0);
            ca.setArticleId(100L);

            PublishArticle existing = new PublishArticle();
            existing.setId(100L);
            existing.setTitle("旧标题");

            when(crawlerArticleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(publishArticleMapper.selectById(100L)).thenReturn(existing);
            when(publishArticleMapper.updateById(any(PublishArticle.class))).thenReturn(1);

            boolean result = txService.publishSingleArticle(ca, 1L);
            assertTrue(result);
            verify(publishArticleMapper).updateById(any(PublishArticle.class));
            verify(publishArticleMapper, never()).insert(any(PublishArticle.class));
        }

        @Test
        @DisplayName("发布异常抛出RuntimeException")
        void publishArticle_exceptionThrown() {
            CrawlerArticle ca = createCrawlerArticle(1L, "测试文章", "AUTO_APPROVED", 0);
            ca.setArticleId(null);

            when(crawlerArticleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(publishArticleMapper.insert(any(PublishArticle.class)))
                    .thenThrow(new RuntimeException("DB connection lost"));

            assertThrows(RuntimeException.class, () -> txService.publishSingleArticle(ca, 1L));
        }

        @Test
        @DisplayName("sourceUrl正确映射到PublishArticle")
        void articleSourceUrlMapping() {
            CrawlerArticle ca = createCrawlerArticle(1L, "测试文章", "AUTO_APPROVED", 0);
            ca.setArticleId(null);
            ca.setSourceUrl("https://mp.weixin.qq.com/s/original-article");
            ca.setSourceName("微信公众号");

            when(crawlerArticleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(publishArticleMapper.insert(any(PublishArticle.class))).thenAnswer(inv -> {
                PublishArticle article = inv.getArgument(0);
                article.setId(100L);
                assertEquals("https://mp.weixin.qq.com/s/original-article", article.getSourceUrl());
                assertEquals("微信公众号", article.getSource());
                return 1;
            });

            txService.publishSingleArticle(ca, 1L);
        }
    }

    // ==================== 产品发布事务测试 ====================

    @Nested
    @DisplayName("产品发布事务")
    class ProductTransactionTests {

        @Test
        @DisplayName("CAS成功-新建robot+关联数据")
        void publishProduct_casSuccess_newInsert() {
            CrawlerProduct cp = createCrawlerProduct(1L, "测试机器人", "AUTO_APPROVED", 0);
            cp.setRobotIdSynced(null);
            cp.setPrice("99999");
            cp.setCategory("人形机器人");
            cp.setGallery("[\"https://img1.jpg\",\"https://img2.jpg\"]");
            cp.setCoverImage("https://cover.jpg");

            when(crawlerProductMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(publishRobotMapper.insert(any(PublishRobot.class))).thenAnswer(inv -> {
                PublishRobot r = inv.getArgument(0);
                r.setId(200L);
                return 1;
            });
            when(publishRobotImageMapper.insert(any(PublishRobotImage.class))).thenReturn(1);
            when(publishRobotPriceMapper.insert(any(PublishRobotPrice.class))).thenReturn(1);
            when(publishRobotTagMapper.insert(any(PublishRobotTag.class))).thenReturn(1);
            when(crawlerMediaMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            boolean result = txService.publishSingleProduct(cp, 1L);
            assertTrue(result);
            verify(publishRobotMapper).insert(any(PublishRobot.class));
            verify(publishRobotImageMapper, atLeast(2)).insert(any(PublishRobotImage.class));
            verify(publishRobotPriceMapper).insert(any(PublishRobotPrice.class));
            verify(publishRobotTagMapper).insert(any(PublishRobotTag.class));
        }

        @Test
        @DisplayName("CAS失败-已被其他线程发布")
        void publishProduct_casFailed() {
            CrawlerProduct cp = createCrawlerProduct(1L, "测试机器人", "AUTO_APPROVED", 0);
            when(crawlerProductMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

            boolean result = txService.publishSingleProduct(cp, 1L);
            assertFalse(result);
            verify(publishRobotMapper, never()).insert(any());
        }

        @Test
        @DisplayName("已有robotIdSynced-更新+删除旧关联+重新写入")
        void publishProduct_republishUpdates() {
            CrawlerProduct cp = createCrawlerProduct(1L, "测试机器人", "AUTO_APPROVED", 0);
            cp.setRobotIdSynced(200L);

            PublishRobot existingRobot = new PublishRobot();
            existingRobot.setId(200L);

            when(crawlerProductMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(publishRobotMapper.selectById(200L)).thenReturn(existingRobot);
            when(publishRobotMapper.updateById(any(PublishRobot.class))).thenReturn(1);
            when(publishRobotImageMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
            when(publishRobotPriceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
            when(publishRobotTagMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
            when(publishRobotVideoMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
            when(publishRobotParamValueMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
            when(crawlerMediaMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            boolean result = txService.publishSingleProduct(cp, 1L);
            assertTrue(result);
            verify(publishRobotMapper).updateById(any(PublishRobot.class));
            verify(publishRobotMapper, never()).insert(any(PublishRobot.class));
            verify(publishRobotImageMapper).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("发布异常抛出RuntimeException")
        void publishProduct_exceptionThrown() {
            CrawlerProduct cp = createCrawlerProduct(1L, "测试机器人", "AUTO_APPROVED", 0);
            cp.setRobotIdSynced(null);

            when(crawlerProductMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(publishRobotMapper.insert(any(PublishRobot.class)))
                    .thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class, () -> txService.publishSingleProduct(cp, 1L));
        }
    }

    // ==================== 失败标记测试 ====================

    @Nested
    @DisplayName("失败标记")
    class MarkFailedTests {

        @Test
        @DisplayName("markArticleFailed-重置synced=0")
        void markArticleFailed() {
            txService.markArticleFailed(1L, "模拟异常");
            verify(crawlerArticleMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("markProductFailed-重置synced=0")
        void markProductFailed() {
            txService.markProductFailed(1L, "模拟异常");
            verify(crawlerProductMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        }
    }

    // ==================== 超时恢复测试 ====================

    @Nested
    @DisplayName("超时恢复")
    class TimeoutRecoveryTests {

        @Test
        @DisplayName("recoverTimedOutPublishing-恢复文章和产品")
        void recoverTimedOut() {
            when(crawlerArticleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(2);
            when(crawlerProductMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            int recovered = txService.recoverTimedOutPublishing(30);
            assertEquals(3, recovered);
        }
    }

    // ==================== 辅助方法 ====================

    private CrawlerArticle createCrawlerArticle(Long id, String title, String status, int synced) {
        CrawlerArticle ca = new CrawlerArticle();
        ca.setId(id);
        ca.setTitle(title);
        ca.setArticleStatus(status);
        ca.setMatchStatus("MATCHED");
        ca.setSynced(synced);
        ca.setSourceUrl("https://example.com/article/" + id);
        ca.setSourceName("测试来源");
        ca.setContentHtml("<p>正文</p>");
        ca.setSummary("摘要");
        ca.setAuthor("作者");
        ca.setTags("[\"标签1\"]");
        ca.setBrandId(1L);
        ca.setCoverImage("https://cover.jpg");
        return ca;
    }

    private CrawlerProduct createCrawlerProduct(Long id, String name, String status, int synced) {
        CrawlerProduct cp = new CrawlerProduct();
        cp.setId(id);
        cp.setProductName(name);
        cp.setProductStatus(status);
        cp.setMatchStatus("MATCHED");
        cp.setSynced(synced);
        cp.setCategoryId(1L);
        cp.setBrandId(1L);
        cp.setModel("Model-X");
        cp.setSummary("简介");
        cp.setCoverImage("https://cover.jpg");
        cp.setPrice("99999");
        cp.setCategory("人形机器人");
        return cp;
    }
}