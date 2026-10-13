package com.robot.home.collector.adapter;

import com.robot.home.collector.extractor.ContentExtractor;
import com.robot.home.collector.fetcher.FetchResult;
import com.robot.home.collector.util.UrlNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 通用网站适配器
 * 适用于大多数企业官网、新闻站等
 * 使用ContentExtractor进行正文提取
 */
@Component
public class GenericWebsiteAdapter implements CrawlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(GenericWebsiteAdapter.class);

    private final ContentExtractor contentExtractor;

    /** 文件扩展名黑名单 */
    private static final Set<String> IGNORE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg", ".ico",
            ".mp4", ".mp3", ".avi", ".mov", ".wmv", ".flv",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".rar", ".7z", ".tar", ".gz",
            ".css", ".js", ".json", ".xml", ".rss",
            ".exe", ".dmg", ".apk"
    ));

    public GenericWebsiteAdapter() {
        this.contentExtractor = new ContentExtractor();
    }

    public GenericWebsiteAdapter(ContentExtractor contentExtractor) {
        this.contentExtractor = contentExtractor;
    }

    @Override
    public String getType() {
        return "generic";
    }

    @Override
    public boolean supports(String url, Map<String, String> config) {
        // 通用适配器，始终可用（优先级最低）
        return true;
    }

    @Override
    public List<String> discoverUrls(String seedUrl, Map<String, String> config) {
        List<String> urls = new ArrayList<>();

        // 尝试发现sitemap
        String sitemapUrl = discoverSitemapUrl(seedUrl);
        if (sitemapUrl != null) {
            urls.add(sitemapUrl);
        }

        // 尝试发现RSS
        String rssUrl = discoverRssUrl(seedUrl);
        if (rssUrl != null) {
            urls.add(rssUrl);
        }

        return urls;
    }

    @Override
    public ParsedData parse(FetchResult fetchResult, Map<String, String> config) {
        if (!fetchResult.isSuccess() || !fetchResult.isHtml()) {
            return ParsedData.fail("Fetch failed or not HTML: " + fetchResult.getStatusCode());
        }

        try {
            ContentExtractor.ExtractResult extractResult =
                    contentExtractor.extract(fetchResult.getHtml(), fetchResult.getUrl());

            ParsedData data = new ParsedData();
            data.setType(detectType(fetchResult.getUrl(), config));
            data.setTitle(extractResult.getTitle());
            data.setContent(extractResult.getContent());
            data.setContentHtml(extractResult.getContentHtml());
            data.setSummary(extractResult.getSummary());
            data.setAuthor(extractResult.getAuthor());
            data.setPublishDate(extractResult.getPublishDate());
            data.setSourceUrl(fetchResult.getUrl());
            data.setTags(extractResult.getTags());
            data.setRawHtml(fetchResult.getHtml());

            // Phase12: 提取封面图(og:image/twitter:image)并合并到images列表
            Document fullDoc = Jsoup.parse(fetchResult.getHtml(), fetchResult.getUrl());
            String coverImage = extractCoverImage(fullDoc);
            List<String> images = new ArrayList<>();
            if (StringUtils.isNotBlank(coverImage)) {
                images.add(coverImage);
                data.addMetadata("coverImage", coverImage);
            }
            if (extractResult.getImages() != null) {
                for (String img : extractResult.getImages()) {
                    if (!img.equals(coverImage)) {
                        images.add(img);
                    }
                }
            }
            data.setImages(images);

            // 提取产品参数（如果有），支持config配置CSS选择器
            extractProductParams(fetchResult.getHtml(), data, config);

            return data;
        } catch (Exception e) {
            log.error("Parse failed for {}: {}", fetchResult.getUrl(), e.getMessage());
            return ParsedData.fail("Parse error: " + e.getMessage());
        }
    }

    @Override
    public List<String> extractLinks(FetchResult fetchResult, Map<String, String> config) {
        List<String> links = new ArrayList<>();

        if (!fetchResult.isSuccess() || !fetchResult.isHtml()) {
            return links;
        }

        try {
            Document doc = Jsoup.parse(fetchResult.getHtml(), fetchResult.getUrl());
            Elements anchors = doc.select("a[href]");

            String includePattern = config != null ? config.get("includePattern") : null;
            String excludePattern = config != null ? config.get("excludePattern") : null;

            for (Element anchor : anchors) {
                String href = anchor.absUrl("href");
                if (StringUtils.isBlank(href)) continue;

                // 规范化URL
                href = UrlNormalizer.normalize(href);

                // 过滤
                if (!shouldIncludeUrl(href, includePattern, excludePattern)) {
                    continue;
                }

                links.add(href);
            }
        } catch (Exception e) {
            log.warn("Extract links failed for {}: {}", fetchResult.getUrl(), e.getMessage());
        }

        return links;
    }

    @Override
    public boolean shouldFollow(String url, int currentDepth, int maxDepth, Map<String, String> config) {
        if (currentDepth >= maxDepth) {
            return false;
        }

        // 过滤文件扩展名
        String lowerUrl = url.toLowerCase();
        for (String ext : IGNORE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) {
                return false;
            }
        }

        // 过滤锚点和javascript
        if (url.contains("#") || url.startsWith("javascript:") || url.startsWith("mailto:")) {
            return false;
        }

        // 应用包含/排除规则
        if (config != null) {
            String includePattern = config.get("includePattern");
            String excludePattern = config.get("excludePattern");
            if (StringUtils.isNotBlank(includePattern) && !UrlNormalizer.matchesInclude(url, includePattern)) {
                return false;
            }
            if (StringUtils.isNotBlank(excludePattern) && UrlNormalizer.matchesExclude(url, excludePattern)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检测内容类型
     */
    private String detectType(String url, Map<String, String> config) {
        if (config != null && config.containsKey("type")) {
            return config.get("type");
        }

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("/news") || lowerUrl.contains("/article") ||
                lowerUrl.contains("/blog") || lowerUrl.contains("/press")) {
            return "article";
        }
        if (lowerUrl.contains("/product") || lowerUrl.contains("/robot") ||
                lowerUrl.contains("/device") || lowerUrl.contains("/solution")) {
            return "product";
        }
        if (lowerUrl.contains("/about") || lowerUrl.contains("/company") ||
                lowerUrl.contains("/contact")) {
            return "company";
        }

        return "article"; // 默认
    }

    /**
     * 提取封面图：优先og:image，其次twitter:image，最后首图
     * Phase12: 补齐A8 GAP，与WeChatAdapter对齐
     * 
     * URL规范化：相对路径/协议相对路径/绝对路径均resolve为完整URL
     */
    private String extractCoverImage(Document doc) {
        // 1. Open Graph image (最可靠的封面图来源)
        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null) {
            String content = ogImage.attr("content");
            if (StringUtils.isNotBlank(content)) {
                String resolved = resolveImageUrl(content, doc);
                if (resolved != null) return resolved;
            }
        }

        // 2. Twitter Card image
        Element twitterImage = doc.selectFirst("meta[name=twitter:image], meta[property=twitter:image]");
        if (twitterImage != null) {
            String content = twitterImage.attr("content");
            if (StringUtils.isNotBlank(content)) {
                String resolved = resolveImageUrl(content, doc);
                if (resolved != null) return resolved;
            }
        }

        // 3. 文章首图（从主要内容区域）
        Element firstImg = doc.selectFirst("article img[src], .content img[src], .article img[src], #content img[src]");
        if (firstImg != null) {
            String src = firstImg.absUrl("src");
            if (StringUtils.isNotBlank(src) && !src.startsWith("data:")) {
                return src;
            }
        }

        return null;
    }

    /**
     * 规范化图片URL：处理相对路径、协议相对路径
     * /images/a.jpg → https://domain/images/a.jpg
     * //cdn.example.com/a.jpg → https://cdn.example.com/a.jpg
     * https://example.com/a.jpg → 原样返回
     */
    private String resolveImageUrl(String url, Document doc) {
        if (StringUtils.isBlank(url)) return null;
        url = url.trim();
        // data: URI不作为封面图
        if (url.startsWith("data:")) return null;
        // 协议相对路径: //cdn.example.com/a.jpg
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        // 绝对路径: https:// 或 http://
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        // 相对路径: /images/a.jpg 或 images/a.jpg
        // 使用Document的baseUri来resolve
        try {
            String baseUri = doc.baseUri();
            if (StringUtils.isNotBlank(baseUri)) {
                if (url.startsWith("/")) {
                    // 绝对路径相对域: /images/a.jpg → https://domain/images/a.jpg
                    int schemeEnd = baseUri.indexOf("://");
                    if (schemeEnd > 0) {
                        int pathStart = baseUri.indexOf("/", schemeEnd + 3);
                        String origin = pathStart > 0 ? baseUri.substring(0, pathStart) : baseUri;
                        return origin + url;
                    }
                } else {
                    // 相对路径相对当前页面: images/a.jpg → https://domain/path/images/a.jpg
                    int lastSlash = baseUri.lastIndexOf("/");
                    if (lastSlash > 0) {
                        return baseUri.substring(0, lastSlash + 1) + url;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve image URL: {} - {}", url, e.getMessage());
        }
        // 无法resolve时返回null(不返回未规范化URL)
        return null;
    }

    /**
     * 提取产品参数表格
     * Phase12: 支持config配置CSS选择器(安全fallback)
     */
    private void extractProductParams(String html, ParsedData data, Map<String, String> config) {
        try {
            Document doc = Jsoup.parse(html);

            // 支持config自定义参数表格选择器(带安全fallback)
            String paramTableSelector = config != null ? config.get("paramTableSelector") : null;
            String paramListSelector = config != null ? config.get("paramListSelector") : null;

            // 查找参数表格(安全选择: 无效选择器不crash)
            String tableSelector = StringUtils.isNotBlank(paramTableSelector)
                    ? paramTableSelector
                    : "table.spec, table.params, table.parameters, .spec-table, .param-table";
            Elements tables = safeSelect(doc, tableSelector);
            for (Element table : tables) {
                Elements rows = table.select("tr");
                for (Element row : rows) {
                    Elements cells = row.select("td, th");
                    if (cells.size() >= 2) {
                        String key = cells.get(0).text().trim();
                        String value = cells.get(1).text().trim();
                        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                            data.addParam(key, value);
                        }
                    }
                }
            }

            // 查找dl参数列表(安全选择: 无效选择器不crash)
            String dlSelector = StringUtils.isNotBlank(paramListSelector)
                    ? paramListSelector
                    : "dl.spec, dl.params, .spec-list, .param-list";
            Elements dls = safeSelect(doc, dlSelector);
            for (Element dl : dls) {
                Elements dts = dl.select("dt");
                Elements dds = dl.select("dd");
                for (int i = 0; i < Math.min(dts.size(), dds.size()); i++) {
                    String key = dts.get(i).text().trim();
                    String value = dds.get(i).text().trim();
                    if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                        data.addParam(key, value);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Product param extraction failed: {}", e.getMessage());
        }
    }

    /**
     * 安全CSS选择: 无效选择器不crash，返回空Elements
     */
    private Elements safeSelect(Document doc, String selector) {
        if (StringUtils.isBlank(selector)) return new Elements();
        try {
            return doc.select(selector);
        } catch (Exception e) {
            log.warn("Invalid CSS selector '{}': {}", selector, e.getMessage());
            return new Elements();
        }
    }

    /**
     * 发现Sitemap URL
     */
    private String discoverSitemapUrl(String baseUrl) {
        try {
            String domain = UrlNormalizer.getDomain(baseUrl);
            if (domain != null) {
                return "https://" + domain + "/sitemap.xml";
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 发现RSS URL
     */
    private String discoverRssUrl(String baseUrl) {
        try {
            String domain = UrlNormalizer.getDomain(baseUrl);
            if (domain != null) {
                return "https://" + domain + "/feed";
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 判断URL是否应包含
     */
    private boolean shouldIncludeUrl(String url, String includePattern, String excludePattern) {
        // 过滤非http/https
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        if (StringUtils.isNotBlank(excludePattern) && UrlNormalizer.matchesExclude(url, excludePattern)) {
            return false;
        }

        return true;
    }
}