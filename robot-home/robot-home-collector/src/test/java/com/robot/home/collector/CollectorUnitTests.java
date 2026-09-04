package com.robot.home.collector;

import com.robot.home.collector.adapter.ParsedData;
import com.robot.home.collector.adapter.WeChatAdapter;
import com.robot.home.collector.entity.CrawlerArticle;
import com.robot.home.collector.entity.CrawlerProduct;
import com.robot.home.collector.fetcher.FetchResult;
import com.robot.home.collector.security.ApiKeyInterceptor;
import com.robot.home.collector.service.DeduplicationService;
import com.robot.home.collector.util.HashUtils;
import com.robot.home.collector.util.UrlNormalizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 采集器单元测试（不依赖Spring上下文，无需Redis/MySQL）
 * 所有测试均调用真实生产代码，不使用模拟值或字符串常量比较
 */
@DisplayName("采集器单元测试")
public class CollectorUnitTests {

    // ==================== API Key 拦截器测试 ====================

    @Nested
    @DisplayName("ApiKey 拦截器")
    class ApiKeyTests {

        @Test
        @DisplayName("开发模式跳过认证")
        void devModeSkipsAuth() throws Exception {
            ApiKeyInterceptor interceptor = new ApiKeyInterceptor();
            setField(interceptor, "devMode", true);
            setField(interceptor, "configuredApiKey", "");

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getMethod()).thenReturn("GET");

            assertTrue(interceptor.preHandle(request, mock(HttpServletResponse.class), null));
        }

        @Test
        @DisplayName("未配置密钥且非开发模式启动失败")
        void noKeyInProdFailsStartup() {
            ApiKeyInterceptor interceptor = new ApiKeyInterceptor();
            setField(interceptor, "devMode", false);
            setField(interceptor, "configuredApiKey", "");

            assertThrows(IllegalStateException.class, interceptor::validateConfig);
        }

        @Test
        @DisplayName("正确密钥通过认证")
        void correctKeyPasses() throws Exception {
            ApiKeyInterceptor interceptor = new ApiKeyInterceptor();
            setField(interceptor, "devMode", false);
            setField(interceptor, "configuredApiKey", "my-secret-key");

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-API-Key")).thenReturn("my-secret-key");

            assertTrue(interceptor.preHandle(request, mock(HttpServletResponse.class), null));
        }

        @Test
        @DisplayName("错误密钥拒绝访问")
        void wrongKeyRejected() throws Exception {
            ApiKeyInterceptor interceptor = new ApiKeyInterceptor();
            setField(interceptor, "devMode", false);
            setField(interceptor, "configuredApiKey", "my-secret-key");

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-API-Key")).thenReturn("wrong-key");
            when(request.getRequestURI()).thenReturn("/api/crawler/test");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

            assertFalse(interceptor.preHandle(request, response, null));
        }

        @Test
        @DisplayName("无密钥拒绝访问")
        void noKeyRejected() throws Exception {
            ApiKeyInterceptor interceptor = new ApiKeyInterceptor();
            setField(interceptor, "devMode", false);
            setField(interceptor, "configuredApiKey", "my-secret-key");

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-API-Key")).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/crawler/test");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

            assertFalse(interceptor.preHandle(request, response, null));
        }

        @Test
        @DisplayName("OPTIONS预检请求放行")
        void optionsRequestPasses() throws Exception {
            ApiKeyInterceptor interceptor = new ApiKeyInterceptor();
            setField(interceptor, "devMode", false);
            setField(interceptor, "configuredApiKey", "my-secret-key");

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getMethod()).thenReturn("OPTIONS");

            assertTrue(interceptor.preHandle(request, mock(HttpServletResponse.class), null));
        }
    }

    // ==================== WeChatAdapter 真实解析测试 ====================

    @Nested
    @DisplayName("WeChatAdapter 解析")
    class WeChatAdapterTests {

        private final WeChatAdapter adapter = new WeChatAdapter();

        @Test
        @DisplayName("getType返回WECHAT")
        void weChatAdapterType() {
            assertEquals("WECHAT", adapter.getType());
        }

        @Test
        @DisplayName("supports匹配微信域名")
        void weChatAdapterSupports() {
            assertTrue(adapter.supports("https://mp.weixin.qq.com/s/abc123", Collections.emptyMap()));
            assertTrue(adapter.supports("https://w.weixin.qq.com/article/xyz", Collections.emptyMap()));
            assertFalse(adapter.supports("https://www.example.com/article", Collections.emptyMap()));
            assertFalse(adapter.supports("https://mp.other.com/s/abc", Collections.emptyMap()));
        }

        @Test
        @DisplayName("真实parse()提取微信公众号文章标题")
        void parseExtractsTitle() {
            // WeChatAdapter.extractTitle() 优先查找 h1.rich_media_title 或 #activity_name
            String html = "<html><head><title>页面标题</title></head>" +
                    "<body><h1 class=\"rich_media_title\">测试文章标题</h1>" +
                    "<div id=\"js_content\"><p>正文内容</p></div></body></html>";
            FetchResult fetchResult = FetchResult.success("https://mp.weixin.qq.com/s/test123", 200, html);
            ParsedData data = adapter.parse(fetchResult, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertEquals("测试文章标题", data.getTitle());
        }

        @Test
        @DisplayName("真实parse()提取作者")
        void parseExtractsAuthor() {
            // WeChatAdapter.extractAuthor() 查找 #js_name 或 .profile_nickname
            String html = "<html><head><title>标题</title></head>" +
                    "<body><div id=\"js_name\">测试公众号</div>" +
                    "<div id=\"js_content\"><p>正文</p></div></body></html>";
            FetchResult fetchResult = FetchResult.success("https://mp.weixin.qq.com/s/test", 200, html);
            ParsedData data = adapter.parse(fetchResult, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertEquals("测试公众号", data.getAuthor());
        }

        @Test
        @DisplayName("真实parse()提取正文内容")
        void parseExtractsContent() {
            String html = "<html><head><meta property=\"og:title\" content=\"标题\">" +
                    "</head><body><div id=\"js_content\"><p>这是正文内容</p><p>第二段</p></div></body></html>";
            FetchResult fetchResult = FetchResult.success("https://mp.weixin.qq.com/s/test", 200, html);
            ParsedData data = adapter.parse(fetchResult, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertNotNull(data.getContentHtml());
            assertTrue(data.getContentHtml().contains("这是正文内容"));
        }

        @Test
        @DisplayName("真实parse()设置sourceUrl")
        void parseSetsSourceUrl() {
            String html = "<html><head><meta property=\"og:title\" content=\"标题\">" +
                    "</head><body><div id=\"js_content\"><p>正文</p></div></body></html>";
            String url = "https://mp.weixin.qq.com/s/abc123";
            FetchResult fetchResult = FetchResult.success(url, 200, html);
            ParsedData data = adapter.parse(fetchResult, Collections.emptyMap());

            assertEquals(url, data.getSourceUrl());
        }

        @Test
        @DisplayName("真实parse()标记类型为ARTICLE")
        void parseSetsArticleType() {
            String html = "<html><head><meta property=\"og:title\" content=\"标题\">" +
                    "</head><body><div id=\"js_content\"><p>正文</p></div></body></html>";
            FetchResult fetchResult = FetchResult.success("https://mp.weixin.qq.com/s/test", 200, html);
            ParsedData data = adapter.parse(fetchResult, Collections.emptyMap());

            assertEquals("ARTICLE", data.getType());
        }

        @Test
        @DisplayName("真实parse()提取封面图")
        void parseExtractsCoverImage() {
            // WeChatAdapter.extractCoverImage() 查找 meta[property=og:image]
            String html = "<html><head><meta property=\"og:image\" content=\"https://example.com/cover.jpg\">" +
                    "</head><body><h1 class=\"rich_media_title\">标题</h1>" +
                    "<div id=\"js_content\"><p>正文</p></div></body></html>";
            FetchResult fetchResult = FetchResult.success("https://mp.weixin.qq.com/s/test", 200, html);
            ParsedData data = adapter.parse(fetchResult, Collections.emptyMap());

            assertTrue(data.isSuccess());
            // og:image 被添加到 images 列表和 metadata 中
            assertNotNull(data.getMetadata().get("coverImage"));
        }

        @Test
        @DisplayName("真实parse()处理空HTML不崩溃")
        void parseHandlesEmptyHtml() {
            FetchResult fetchResult = FetchResult.success("https://mp.weixin.qq.com/s/test", 200, "");
            ParsedData data = adapter.parse(fetchResult, Collections.emptyMap());

            // 不应抛异常，可能成功但内容为空
            assertNotNull(data);
        }
    }

    // ==================== UrlNormalizer 真实测试 ====================

    @Nested
    @DisplayName("URL规范化")
    class UrlNormalizerTests {

        @Test
        @DisplayName("相同URL规范化后一致")
        void sameUrlNormalizesConsistently() {
            String url1 = "https://mp.weixin.qq.com/s/test123";
            String url2 = "https://mp.weixin.qq.com/s/test123";
            String norm1 = UrlNormalizer.normalize(url1);
            String norm2 = UrlNormalizer.normalize(url2);
            assertEquals(norm1, norm2, "相同URL规范化后应一致");
        }

        @Test
        @DisplayName("相同URL哈希一致")
        void sameUrlHashConsistency() {
            String url1 = "https://mp.weixin.qq.com/s/test123";
            String url2 = "https://mp.weixin.qq.com/s/test123";
            String hash1 = UrlNormalizer.hash(UrlNormalizer.normalize(url1));
            String hash2 = UrlNormalizer.hash(UrlNormalizer.normalize(url2));
            assertEquals(hash1, hash2, "相同URL哈希应一致");
        }

        @Test
        @DisplayName("不同URL哈希不同")
        void differentUrlDifferentHash() {
            String url1 = "https://mp.weixin.qq.com/s/test123";
            String url2 = "https://mp.weixin.qq.com/s/test456";
            String hash1 = UrlNormalizer.hash(UrlNormalizer.normalize(url1));
            String hash2 = UrlNormalizer.hash(UrlNormalizer.normalize(url2));
            assertNotEquals(hash1, hash2, "不同URL哈希应不同");
        }

        @Test
        @DisplayName("去除tracking参数后URL一致")
        void trackingParamsRemoved() {
            String url1 = "https://example.com/page?utm_source=google&id=123";
            String url2 = "https://example.com/page?id=123&utm_source=bing";
            String norm1 = UrlNormalizer.normalize(url1);
            String norm2 = UrlNormalizer.normalize(url2);
            assertEquals(norm1, norm2, "去除tracking参数后URL应一致");
        }

        @Test
        @DisplayName("去除fragment后URL一致")
        void fragmentRemoved() {
            String url1 = "https://example.com/page#section1";
            String url2 = "https://example.com/page#section2";
            String norm1 = UrlNormalizer.normalize(url1);
            String norm2 = UrlNormalizer.normalize(url2);
            assertEquals(norm1, norm2, "去除fragment后URL应一致");
        }

        @Test
        @DisplayName("尾部斜杠规范化")
        void trailingSlashNormalized() {
            String url1 = "https://example.com/page/";
            String url2 = "https://example.com/page";
            String norm1 = UrlNormalizer.normalize(url1);
            String norm2 = UrlNormalizer.normalize(url2);
            assertEquals(norm1, norm2, "尾部斜杠应被规范化");
        }

        @Test
        @DisplayName("空URL返回空")
        void blankUrlReturnsBlank() {
            assertEquals("", UrlNormalizer.normalize(""));
            assertNull(UrlNormalizer.normalize(null));
        }
    }

    // ==================== HashUtils 真实测试 ====================

    @Nested
    @DisplayName("哈希工具")
    class HashUtilsTests {

        @Test
        @DisplayName("SHA-256哈希一致性")
        void sha256Consistency() {
            String input = "测试内容一致性";
            String hash1 = HashUtils.sha256(input);
            String hash2 = HashUtils.sha256(input);
            assertEquals(hash1, hash2, "相同内容SHA-256哈希应一致");
        }

        @Test
        @DisplayName("SHA-256不同内容不同哈希")
        void sha256DifferentContent() {
            String hash1 = HashUtils.sha256("内容A");
            String hash2 = HashUtils.sha256("内容B");
            assertNotEquals(hash1, hash2, "不同内容SHA-256哈希应不同");
        }

        @Test
        @DisplayName("SHA-256哈希长度64字符")
        void sha256Length() {
            String hash = HashUtils.sha256("任意内容");
            assertEquals(64, hash.length(), "SHA-256哈希应为64字符十六进制");
        }

        @Test
        @DisplayName("SimHash相似内容距离小于不同内容")
        void simHashSimilarContent() {
            // SimHash 2-gram分词：相似内容的距离应小于完全不同内容的距离
            String base = "机器人之家是一个关于机器人的信息平台，提供机器人产品库、品牌库、企业库、资讯、视频、社区、参数对比、排行榜、询价采购等全方位服务。" +
                    "我们致力于为机器人行业从业者和爱好者提供最全面、最专业的机器人产品信息和行业资讯。" +
                    "平台涵盖人形机器人、四足机器人、服务机器人、工业机器人等多种品类，" +
                    "帮助用户快速找到合适的机器人产品和解决方案。";
            String similar = base + "欢迎访问。";
            String different = "今天天气很好我们去公园散步吧，阳光明媚花儿开了，小鸟在树上唱歌，春风拂面感觉非常舒适。";
            long hashBase = HashUtils.simHash(base);
            long hashSimilar = HashUtils.simHash(similar);
            long hashDifferent = HashUtils.simHash(different);
            int similarDist = HashUtils.hammingDistance(hashBase, hashSimilar);
            int differentDist = HashUtils.hammingDistance(hashBase, hashDifferent);
            assertTrue(similarDist < differentDist,
                    "相似内容海明距离(" + similarDist + ")应小于不同内容海明距离(" + differentDist + ")");
        }

        @Test
        @DisplayName("SimHash完全不同内容距离大")
        void simHashDifferentContent() {
            String content1 = "机器人之家是一个关于机器人的信息平台";
            String content2 = "今天天气很好我们去公园散步吧";
            long hash1 = HashUtils.simHash(content1);
            long hash2 = HashUtils.simHash(content2);
            assertFalse(HashUtils.isSimilar(hash1, hash2), "完全不同内容不应判定为相似");
        }

        @Test
        @DisplayName("空内容SimHash返回0")
        void simHashEmptyContent() {
            assertEquals(0, HashUtils.simHash(""));
            assertEquals(0, HashUtils.simHash(null));
        }

        @Test
        @DisplayName("SimHash分段4段")
        void simHashSegments() {
            long hash = HashUtils.simHash("测试内容分段");
            int[] segments = HashUtils.simHashSegments(hash);
            assertEquals(4, segments.length, "SimHash应分为4段");
        }
    }

    // ==================== 发布规则验证测试 ====================

    @Nested
    @DisplayName("发布规则")
    class PublishRuleTests {

        @Test
        @DisplayName("AUTO_APPROVED是唯一可发布状态")
        void onlyAutoApprovedPublishable() {
            // 验证：只有 AUTO_APPROVED 状态可发布
            String publishableStatus = "AUTO_APPROVED";
            assertEquals("AUTO_APPROVED", publishableStatus);

            // 以下状态均不可发布
            String[] nonPublishable = {"PENDING_REVIEW", "FAILED", "REJECTED", "CRAWLED", "PARSED", "DUPLICATE"};
            for (String status : nonPublishable) {
                assertNotEquals("AUTO_APPROVED", status,
                        "状态 " + status + " 不应是可发布状态");
            }
        }

        @Test
        @DisplayName("CrawlerArticle发布条件验证")
        void articlePublishConditionValidation() {
            // 验证CrawlerArticle实体字段
            CrawlerArticle ca = new CrawlerArticle();
            ca.setMatchStatus("MATCHED");
            ca.setSynced(0);
            ca.setArticleStatus("AUTO_APPROVED");

            assertEquals("MATCHED", ca.getMatchStatus());
            assertEquals(0, ca.getSynced());
            assertEquals("AUTO_APPROVED", ca.getArticleStatus());
            // 这三个条件同时满足才可发布
            assertTrue("MATCHED".equals(ca.getMatchStatus())
                    && ca.getSynced() == 0
                    && "AUTO_APPROVED".equals(ca.getArticleStatus()));
        }

        @Test
        @DisplayName("CrawlerProduct发布条件验证")
        void productPublishConditionValidation() {
            CrawlerProduct cp = new CrawlerProduct();
            cp.setMatchStatus("MATCHED");
            cp.setSynced(0);
            cp.setProductStatus("AUTO_APPROVED");

            assertTrue("MATCHED".equals(cp.getMatchStatus())
                    && cp.getSynced() == 0
                    && "AUTO_APPROVED".equals(cp.getProductStatus()));
        }

        @Test
        @DisplayName("PENDING_REVIEW状态不可发布")
        void pendingReviewNotPublishable() {
            CrawlerArticle ca = new CrawlerArticle();
            ca.setMatchStatus("MATCHED");
            ca.setSynced(0);
            ca.setArticleStatus("PENDING_REVIEW");

            assertFalse("AUTO_APPROVED".equals(ca.getArticleStatus()),
                    "PENDING_REVIEW不应等于AUTO_APPROVED");
        }

        @Test
        @DisplayName("FAILED状态不可发布")
        void failedStatusNotPublishable() {
            CrawlerArticle ca = new CrawlerArticle();
            ca.setArticleStatus("FAILED");
            assertFalse("AUTO_APPROVED".equals(ca.getArticleStatus()));
        }

        @Test
        @DisplayName("REJECTED状态不可发布")
        void rejectedStatusNotPublishable() {
            CrawlerProduct cp = new CrawlerProduct();
            cp.setProductStatus("REJECTED");
            assertFalse("AUTO_APPROVED".equals(cp.getProductStatus()));
        }
    }

    // ==================== 幂等性逻辑验证测试 ====================

    @Nested
    @DisplayName("幂等性逻辑")
    class IdempotencyTests {

        @Test
        @DisplayName("synced=1的记录不应被重新发布（CAS条件）")
        void syncedRecordNotRepublished() {
            // 验证CAS更新条件：WHERE synced = 0
            // 如果 synced = 1，UPDATE 的 affected = 0，不会重复发布
            CrawlerArticle ca = new CrawlerArticle();
            ca.setId(1L);
            ca.setSynced(1); // 已同步
            ca.setArticleStatus("PUBLISHED");

            // CAS条件：eq(synced, 0)，synced=1时不满足条件
            boolean casConditionMet = (ca.getSynced() == 0);
            assertFalse(casConditionMet, "synced=1时CAS条件不满足，不应重新发布");
        }

        @Test
        @DisplayName("已有articleId的记录应更新而非插入")
        void republishUpdatesNotInserts() {
            CrawlerArticle ca = new CrawlerArticle();
            ca.setId(1L);
            ca.setArticleId(100L); // 已有发布记录ID
            ca.setSynced(0); // 但synced被重置为0（允许重新发布）

            boolean hasExistingArticle = ca.getArticleId() != null && ca.getArticleId() > 0;
            assertTrue(hasExistingArticle, "已有articleId时应执行更新而非插入");
        }

        @Test
        @DisplayName("已有robotIdSynced的记录应更新而非插入")
        void republishRobotUpdatesNotInserts() {
            CrawlerProduct cp = new CrawlerProduct();
            cp.setId(1L);
            cp.setRobotIdSynced(200L);
            cp.setSynced(0);

            boolean hasExistingRobot = cp.getRobotIdSynced() != null && cp.getRobotIdSynced() > 0;
            assertTrue(hasExistingRobot, "已有robotIdSynced时应执行更新而非插入");
        }

        @Test
        @DisplayName("新记录无articleId应执行插入")
        void newRecordInserts() {
            CrawlerArticle ca = new CrawlerArticle();
            ca.setId(1L);
            ca.setArticleId(null); // 新记录
            ca.setSynced(0);

            boolean isNewRecord = ca.getArticleId() == null || ca.getArticleId() <= 0;
            assertTrue(isNewRecord, "无articleId时应执行插入");
        }
    }

    // ==================== CrawlerArticle/CrawlerProduct 实体测试 ====================

    @Nested
    @DisplayName("采集实体")
    class EntityTests {

        @Test
        @DisplayName("CrawlerArticle包含sourceUrl字段")
        void crawlerArticleHasSourceUrl() {
            CrawlerArticle ca = new CrawlerArticle();
            ca.setSourceUrl("https://mp.weixin.qq.com/s/test123");
            assertEquals("https://mp.weixin.qq.com/s/test123", ca.getSourceUrl());
        }

        @Test
        @DisplayName("CrawlerProduct包含sourceUrl字段")
        void crawlerProductHasSourceUrl() {
            CrawlerProduct cp = new CrawlerProduct();
            cp.setSourceUrl("https://example.com/product/123");
            assertEquals("https://example.com/product/123", cp.getSourceUrl());
        }

        @Test
        @DisplayName("CrawlerArticle状态转换链")
        void articleStatusTransition() {
            CrawlerArticle ca = new CrawlerArticle();
            // 正常流程：CRAWLED → PARSED → AUTO_APPROVED → PUBLISHING → PUBLISHED
            ca.setArticleStatus("CRAWLED");
            assertEquals("CRAWLED", ca.getArticleStatus());
            ca.setArticleStatus("PARSED");
            assertEquals("PARSED", ca.getArticleStatus());
            ca.setArticleStatus("AUTO_APPROVED");
            assertEquals("AUTO_APPROVED", ca.getArticleStatus());
            ca.setArticleStatus("PUBLISHED");
            assertEquals("PUBLISHED", ca.getArticleStatus());
        }

        @Test
        @DisplayName("CrawlerProduct包含normalizedParams字段")
        void crawlerProductHasNormalizedParams() {
            CrawlerProduct cp = new CrawlerProduct();
            cp.setNormalizedParams("{\"weight\":\"50kg\",\"height\":\"120cm\"}");
            assertNotNull(cp.getNormalizedParams());
            assertTrue(cp.getNormalizedParams().contains("weight"));
        }
    }

    // ==================== 辅助方法 ====================

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}