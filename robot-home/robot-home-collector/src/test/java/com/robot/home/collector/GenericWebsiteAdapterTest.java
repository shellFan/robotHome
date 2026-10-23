package com.robot.home.collector;

import com.robot.home.collector.adapter.GenericWebsiteAdapter;
import com.robot.home.collector.adapter.ParsedData;
import com.robot.home.collector.fetcher.FetchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenericWebsiteAdapter 解析质量测试
 * Night 4 Stage 4: 真实HTML采集解析验证（本地fixture，不依赖网络）
 */
class GenericWebsiteAdapterTest {

    private final GenericWebsiteAdapter adapter = new GenericWebsiteAdapter();

    // ==================== parse() 正文提取 ====================

    @Nested
    @DisplayName("GenericWebsiteAdapter parse() 解析质量")
    class ParseQualityTests {

        @Test
        @DisplayName("parse()提取h1标题")
        void parseExtractsTitle() {
            String html = "<html><head><title>页面标题</title></head><body>" +
                    "<article><h1>协作机器人新品发布</h1>" +
                    "<p>这是一段足够长的正文内容用于测试正文提取功能是否正常工作。" +
                    "需要超过最小内容长度阈值才能被认定为有效正文区域。" +
                    "补充更多文本来确保长度达标，避免触发og:description回退逻辑。</p>" +
                    "</article></body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/news/1", 200, html);
            ParsedData data = adapter.parse(fr, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertEquals("协作机器人新品发布", data.getTitle());
        }

        @Test
        @DisplayName("parse()提取article正文内容")
        void parseExtractsContent() {
            String body = "<p>协作机器人是面向工业场景的新型机器人产品，具有安全、灵活、易于部署等特点。" +
                    "近年来随着人力成本上升和柔性制造需求增长，协作机器人市场规模持续扩大。" +
                    "本文将详细介绍协作机器人的技术特点、应用场景和选型建议。</p>";
            String html = "<html><body><article><h1>协作机器人指南</h1>" + body + "</article></body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/article/1", 200, html);
            ParsedData data = adapter.parse(fr, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertNotNull(data.getContent());
            assertTrue(data.getContent().contains("协作机器人是面向工业场景的新型机器人产品"));
            assertNotNull(data.getContentHtml());
            assertTrue(data.getContentHtml().contains("协作机器人"));
        }

        @Test
        @DisplayName("parse() removes cookie consent overlay from article body")
        void parseRemovesCookieConsentOverlay() {
            String articleText = "协作机器人面向工业场景，强调安全、灵活和易于部署。" +
                    "企业可以根据工作空间、负载、精度和节拍选择合适的型号。" +
                    "在实际部署中，还需要评估末端工具、工位布局和人员培训，确保机器人能够稳定地融入生产流程。" +
                    "本文介绍常见的选型因素，并说明如何通过小规模试点验证投资回报。";
            String html = "<html><body><article><h1>协作机器人选型指南</h1>" +
                    "<div id='cookie-consent-banner'>We use cookies to improve your experience. " +
                    "Accept all cookies or change your cookie preferences." +
                    "<img src='/cookie-banner.png' alt='cookie banner'></div>" +
                    "<div class='article-content'><p>" + articleText + "</p></div>" +
                    "</article></body></html>";
            FetchResult fetchResult = FetchResult.success("https://www.example.com/news/consent", 200, html);

            ParsedData data = adapter.parse(fetchResult, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertTrue(data.getContent().contains("协作机器人面向工业场景"));
            assertFalse(data.getContent().toLowerCase().contains("we use cookies"));
            assertFalse(data.getContent().toLowerCase().contains("accept all cookies"));
            assertFalse(data.getImages().contains("https://www.example.com/cookie-banner.png"));
        }

        @Test
        @DisplayName("parse()非HTML返回fail")
        void parseFailsOnNonHtml() {
            FetchResult fr = new FetchResult();
            fr.setUrl("https://www.example.com/data.json");
            fr.setStatusCode(200);
            fr.setContentType("application/json");
            fr.setHtml("{\"key\":\"value\"}");

            ParsedData data = adapter.parse(fr, Collections.emptyMap());
            assertFalse(data.isSuccess());
        }

        @Test
        @DisplayName("parse()空HTML不崩溃")
        void parseHandlesEmptyHtml() {
            FetchResult fr = FetchResult.success("https://www.example.com/blank", 200, "");
            ParsedData data = adapter.parse(fr, Collections.emptyMap());
            assertNotNull(data);
        }
    }

    // ==================== 封面图提取与URL规范化 ====================

    @Nested
    @DisplayName("封面图提取与URL规范化")
    class CoverImageTests {

        @Test
        @DisplayName("og:image绝对路径直接使用")
        void coverImageAbsoluteUrl() {
            String html = "<html><head>" +
                    "<meta property=\"og:image\" content=\"https://cdn.example.com/cover.jpg\">" +
                    "</head><body><article><h1>标题</h1><p>正文内容足够长用于通过最小长度检查。" +
                    "继续补充文本确保超过阈值限制避免回退到og:description元数据。</p></article>" +
                    "</body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/news/2", 200, html);
            ParsedData data = adapter.parse(fr, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertNotNull(data.getImages());
            assertFalse(data.getImages().isEmpty());
            assertEquals("https://cdn.example.com/cover.jpg", data.getImages().get(0));
        }

        @Test
        @DisplayName("og:image协议相对路径规范化为https")
        void coverImageProtocolRelativeUrl() {
            String html = "<html><head>" +
                    "<meta property=\"og:image\" content=\"//cdn.example.com/img/cover.jpg\">" +
                    "</head><body><article><h1>标题</h1><p>正文内容足够长用于通过最小长度检查。" +
                    "继续补充文本确保超过阈值限制避免回退到og:description元数据。</p></article>" +
                    "</body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/news/3", 200, html);
            ParsedData data = adapter.parse(fr, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertFalse(data.getImages().isEmpty());
            assertEquals("https://cdn.example.com/img/cover.jpg", data.getImages().get(0));
        }

        @Test
        @DisplayName("og:image相对路径基于baseUri规范化")
        void coverImageRelativeUrl() {
            String html = "<html><head>" +
                    "<meta property=\"og:image\" content=\"/images/products/robot-x.jpg\">" +
                    "</head><body><article><h1>标题</h1><p>正文内容足够长用于通过最小长度检查。" +
                    "继续补充文本确保超过阈值限制避免回退到og:description元数据。</p></article>" +
                    "</body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/product/robot-x", 200, html);
            ParsedData data = adapter.parse(fr, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertFalse(data.getImages().isEmpty());
            assertEquals("https://www.example.com/images/products/robot-x.jpg", data.getImages().get(0));
        }
    }

    // ==================== 产品参数提取 ====================

    @Nested
    @DisplayName("产品参数表格提取")
    class ProductParamTests {

        @Test
        @DisplayName("table.spec参数表格提取")
        void extractsTableSpecParams() {
            String html = "<html><body><article><h1>机器人产品</h1>" +
                    "<p>产品介绍正文内容足够长用于通过最小长度检查阈值。" +
                    "继续补充文本确保超过限制避免回退逻辑干扰测试结果。</p>" +
                    "<table class=\"spec\"><tr><th>型号</th><td>XR-10</td></tr>" +
                    "<tr><th>负载</th><td>10kg</td></tr>" +
                    "<tr><th>工作半径</th><td>1300mm</td></tr></table>" +
                    "</article></body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/product/xr10", 200, html);
            ParsedData data = adapter.parse(fr, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertNotNull(data.getParams());
            assertEquals("XR-10", data.getParams().get("型号"));
            assertEquals("10kg", data.getParams().get("负载"));
            assertEquals("1300mm", data.getParams().get("工作半径"));
        }

        @Test
        @DisplayName("dl参数列表提取")
        void extractsDlParams() {
            String html = "<html><body><article><h1>机器人产品</h1>" +
                    "<p>产品介绍正文内容足够长用于通过最小长度检查阈值。" +
                    "继续补充文本确保超过限制避免回退逻辑干扰测试结果。</p>" +
                    "<dl class=\"spec\"><dt>重复定位精度</dt><dd>±0.02mm</dd>" +
                    "<dt>防护等级</dt><dd>IP54</dd></dl>" +
                    "</article></body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/product/xr10", 200, html);
            ParsedData data = adapter.parse(fr, Collections.emptyMap());

            assertTrue(data.isSuccess());
            assertNotNull(data.getParams());
            assertEquals("±0.02mm", data.getParams().get("重复定位精度"));
            assertEquals("IP54", data.getParams().get("防护等级"));
        }
    }

    // ==================== 链接提取与过滤 ====================

    @Nested
    @DisplayName("链接提取与shouldFollow过滤")
    class LinkExtractionTests {

        @Test
        @DisplayName("extractLinks提取并规范化内部链接")
        void extractsInternalLinks() {
            String html = "<html><body>" +
                    "<a href=\"/news/1\">新闻1</a>" +
                    "<a href=\"https://www.example.com/about\">关于</a>" +
                    "<a href=\"javascript:void(0)\">无效</a>" +
                    "</body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/", 200, html);
            List<String> links = adapter.extractLinks(fr, Collections.emptyMap());

            assertNotNull(links);
            // javascript:被过滤，剩下2个http链接
            assertTrue(links.size() >= 2);
            assertTrue(links.stream().anyMatch(l -> l.contains("/news/1")));
            assertTrue(links.stream().anyMatch(l -> l.contains("/about")));
            assertTrue(links.stream().noneMatch(l -> l.startsWith("javascript")));
        }

        @Test
        @DisplayName("shouldFollow过滤文件扩展名")
        void shouldFollowFiltersExtensions() {
            Map<String, String> config = Collections.emptyMap();
            assertFalse(adapter.shouldFollow("https://example.com/img.jpg", 0, 3, config));
            assertFalse(adapter.shouldFollow("https://example.com/doc.pdf", 0, 3, config));
            assertFalse(adapter.shouldFollow("https://example.com/file.zip", 0, 3, config));
            assertTrue(adapter.shouldFollow("https://example.com/news/1", 0, 3, config));
        }

        @Test
        @DisplayName("shouldFollow过滤锚点和特殊协议")
        void shouldFollowFiltersAnchors() {
            Map<String, String> config = Collections.emptyMap();
            assertFalse(adapter.shouldFollow("https://example.com/page#section", 0, 3, config));
            assertFalse(adapter.shouldFollow("javascript:void(0)", 0, 3, config));
            assertFalse(adapter.shouldFollow("mailto:test@example.com", 0, 3, config));
        }

        @Test
        @DisplayName("shouldFollow深度限制")
        void shouldFollowDepthLimit() {
            Map<String, String> config = Collections.emptyMap();
            assertFalse(adapter.shouldFollow("https://example.com/page", 3, 3, config));
            assertTrue(adapter.shouldFollow("https://example.com/page", 2, 3, config));
        }
    }

    // ==================== 内容类型推断 ====================

    @Nested
    @DisplayName("内容类型推断")
    class DetectTypeTests {

        @Test
        @DisplayName("URL含/product推断为product类型")
        void detectsProductType() {
            String html = "<html><body><article><h1>产品页</h1>" +
                    "<p>产品介绍正文内容足够长用于通过最小长度检查阈值。" +
                    "继续补充文本确保超过限制避免回退逻辑干扰测试结果。</p>" +
                    "</article></body></html>";
            FetchResult fr = FetchResult.success("https://www.example.com/product/xr10", 200, html);
            ParsedData data = adapter.parse(fr, Collections.emptyMap());

            assertEquals("product", data.getType());
        }

        @Test
        @DisplayName("config指定type时优先使用config")
        void configTypeOverrides() {
            String html = "<html><body><article><h1>页面</h1>" +
                    "<p>正文内容足够长用于通过最小长度检查阈值。" +
                    "继续补充文本确保超过限制避免回退逻辑干扰测试结果。</p>" +
                    "</article></body></html>";
            Map<String, String> config = new HashMap<>();
            config.put("type", "product");
            FetchResult fr = FetchResult.success("https://www.example.com/news/9", 200, html);
            ParsedData data = adapter.parse(fr, config);

            assertEquals("product", data.getType());
        }
    }
}
