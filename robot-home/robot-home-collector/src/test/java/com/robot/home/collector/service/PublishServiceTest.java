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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PublishService 单元测试（Mockito）
 * 测试编排层逻辑：批量发布、失败处理、超时恢复
 *
 * ⚠️ 本测试不验证 SQL 正确性和事务回滚，那由 PublishIntegrationTest 负责
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("发布服务编排层测试")
public class PublishServiceTest {

    @Mock
    private CrawlerArticleMapper crawlerArticleMapper;

    @Mock
    private CrawlerProductMapper crawlerProductMapper;

    @Mock
    private PublishTransactionService txService;

    @InjectMocks
    private PublishService publishService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publishService, "defaultArticleCategoryId", 1L);
        ReflectionTestUtils.setField(publishService, "publishTimeoutMinutes", 30);
    }

    // ==================== 文章批量发布编排测试 ====================

    @Nested
    @DisplayName("文章批量发布编排")
    class ArticlePublishOrchestrationTests {

        @Test
        @DisplayName("无待发布文章返回0")
        void noArticlesToPublish() {
            when(crawlerArticleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            int count = publishService.publishArticles();
            assertEquals(0, count);
            verify(txService, never()).publishSingleArticle(any(), anyLong());
        }

        @Test
        @DisplayName("批量发布-全部成功")
        void batchPublishAllSuccess() {
            CrawlerArticle ca1 = createCrawlerArticle(1L, "文章1", "AUTO_APPROVED", 0);
            CrawlerArticle ca2 = createCrawlerArticle(2L, "文章2", "AUTO_APPROVED", 0);

            when(crawlerArticleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(ca1, ca2));
            when(txService.publishSingleArticle(any(CrawlerArticle.class), anyLong())).thenReturn(true);

            int count = publishService.publishArticles();
            assertEquals(2, count);
            verify(txService, times(1)).recoverTimedOutPublishing(30);
            verify(txService, times(2)).publishSingleArticle(any(CrawlerArticle.class), eq(1L));
        }

        @Test
        @DisplayName("批量发布-单条失败不阻塞其他-标记FAILED")
        void batchPublish_partialFailure() {
            CrawlerArticle ca1 = createCrawlerArticle(1L, "成功文章", "AUTO_APPROVED", 0);
            CrawlerArticle ca2 = createCrawlerArticle(2L, "失败文章", "AUTO_APPROVED", 0);

            when(crawlerArticleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(ca1, ca2));
            when(txService.publishSingleArticle(eq(ca1), anyLong())).thenReturn(true);
            when(txService.publishSingleArticle(eq(ca2), anyLong()))
                    .thenThrow(new RuntimeException("DB error"));

            int count = publishService.publishArticles();
            assertEquals(1, count);
            verify(txService).markArticleFailed(eq(2L), contains("DB error"));
        }

        @Test
        @DisplayName("CAS失败(已被其他线程发布)不计入失败")
        void batchPublish_casFailedNotError() {
            CrawlerArticle ca1 = createCrawlerArticle(1L, "文章1", "AUTO_APPROVED", 0);

            when(crawlerArticleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(ca1));
            when(txService.publishSingleArticle(any(CrawlerArticle.class), anyLong())).thenReturn(false);

            int count = publishService.publishArticles();
            assertEquals(0, count);
            verify(txService, never()).markArticleFailed(anyLong(), anyString());
        }
    }

    // ==================== 产品批量发布编排测试 ====================

    @Nested
    @DisplayName("产品批量发布编排")
    class ProductPublishOrchestrationTests {

        @Test
        @DisplayName("无待发布产品返回0")
        void noProductsToPublish() {
            when(crawlerProductMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            int count = publishService.publishProducts();
            assertEquals(0, count);
            verify(txService, never()).publishSingleProduct(any(), anyLong());
        }

        @Test
        @DisplayName("批量发布-单条失败标记FAILED")
        void batchPublish_partialFailure() {
            CrawlerProduct cp1 = createCrawlerProduct(1L, "成功产品", "AUTO_APPROVED", 0);
            CrawlerProduct cp2 = createCrawlerProduct(2L, "失败产品", "AUTO_APPROVED", 0);

            when(crawlerProductMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(cp1, cp2));
            when(txService.publishSingleProduct(eq(cp1), anyLong())).thenReturn(true);
            when(txService.publishSingleProduct(eq(cp2), anyLong()))
                    .thenThrow(new RuntimeException("Connection timeout"));

            int count = publishService.publishProducts();
            assertEquals(1, count);
            verify(txService).markProductFailed(eq(2L), contains("timeout"));
        }
    }

    // ==================== 超时恢复编排测试 ====================

    @Nested
    @DisplayName("超时恢复编排")
    class TimeoutRecoveryTests {

        @Test
        @DisplayName("发布前先恢复超时PUBLISHING记录")
        void recoverBeforePublish() {
            when(crawlerArticleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            publishService.publishArticles();

            verify(txService).recoverTimedOutPublishing(30);
        }
    }

    // ==================== 待发布计数测试 ====================

    @Nested
    @DisplayName("待发布计数")
    class PendingCountTests {

        @Test
        @DisplayName("获取待发布文章数量")
        void getPendingArticleCount() {
            when(crawlerArticleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
            assertEquals(5L, publishService.getPendingArticleCount());
        }

        @Test
        @DisplayName("获取待发布产品数量")
        void getPendingProductCount() {
            when(crawlerProductMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
            assertEquals(3L, publishService.getPendingProductCount());
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
        return cp;
    }
}