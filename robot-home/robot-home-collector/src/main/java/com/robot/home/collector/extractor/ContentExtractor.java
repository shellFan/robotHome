package com.robot.home.collector.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 通用正文提取器
 * 基于DOM特征、文本密度、链接密度、标签权重等多策略融合
 * 适用于新闻、产品、博客等多种页面类型
 */
public class ContentExtractor {

    private static final Logger log = LoggerFactory.getLogger(ContentExtractor.class);

    /** 低质量内容标签 */
    private static final Set<String> NOISE_TAGS = new HashSet<>(Arrays.asList(
            "script", "style", "nav", "header", "footer", "aside",
            "iframe", "noscript", "form", "button", "input",
            "select", "textarea", "label", "svg"
    ));

    /** 正文候选标签（权重从高到低） */
    private static final String[] CONTENT_SELECTORS = {
            "article", "[role='main']", ".article-content", ".post-content",
            ".entry-content", ".content-body", ".news-content", ".product-description",
            ".detail-content", ".article-body", ".post-body", ".story-body",
            ".main-content", "#content", "#main", ".content", ".main"
    };

    /** 标题选择器 */
    private static final String[] TITLE_SELECTORS = {
            "h1", ".article-title", ".post-title", ".entry-title",
            ".news-title", ".title", "[property='og:title']"
    };

    /** 发布时间选择器 */
    private static final String[] DATE_SELECTORS = {
            "time", "[property='article:published_time']",
            ".article-date", ".post-date", ".publish-date",
            ".date", "[datetime]"
    };

    /** 作者选择器 */
    private static final String[] AUTHOR_SELECTORS = {
            "[property='article:author']", ".article-author",
            ".post-author", ".author", "[rel='author']"
    };

    /** 图片选择器 */
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\d{4}[-/.年]\\d{1,2}[-/.月]\\d{1,2}[日号]?");

    private int minContentLength = 200;
    private double minTextDensity = 0.1;
    private double maxLinkDensity = 0.5;

    public ContentExtractor() {
    }

    public ContentExtractor(int minContentLength, double minTextDensity, double maxLinkDensity) {
        this.minContentLength = minContentLength;
        this.minTextDensity = minTextDensity;
        this.maxLinkDensity = maxLinkDensity;
    }

    /**
     * 提取结果
     */
    public static class ExtractResult {
        private String title;
        private String content;       // 纯文本
        private String contentHtml;   // HTML格式正文
        private String author;
        private String publishDate;
        private List<String> images = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        private String summary;       // 摘要

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getContentHtml() { return contentHtml; }
        public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getPublishDate() { return publishDate; }
        public void setPublishDate(String publishDate) { this.publishDate = publishDate; }
        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    /**
     * 从HTML提取正文
     */
    public ExtractResult extract(String html, String url) {
        ExtractResult result = new ExtractResult();

        if (StringUtils.isBlank(html)) {
            return result;
        }

        Document doc = Jsoup.parse(html, url);

        // 1. 提取标题
        result.setTitle(extractTitle(doc));

        // 2. 提取元数据
        result.setAuthor(extractAuthor(doc));
        result.setPublishDate(extractDate(doc));
        result.setTags(extractTags(doc));

        // 3. 提取正文（多策略）
        Element contentElement = findContentElement(doc);
        if (contentElement != null) {
            // 提取图片
            result.setImages(extractImages(contentElement, url));
            // 清理噪声
            cleanNoise(contentElement);
            // HTML正文
            result.setContentHtml(contentElement.html());
            // 纯文本
            result.setContent(cleanText(contentElement));
        }

        // 4. 如果正文太短，尝试OG meta
        if (StringUtils.isBlank(result.getContent()) || result.getContent().length() < minContentLength) {
            String ogDesc = doc.selectFirst("meta[property=og:description]") != null
                    ? doc.selectFirst("meta[property=og:description]").attr("content") : null;
            if (StringUtils.isNotBlank(ogDesc) && ogDesc.length() > (result.getContent() != null ? result.getContent().length() : 0)) {
                result.setContent(ogDesc);
                result.setContentHtml("<p>" + ogDesc + "</p>");
            }
        }

        // 5. 生成摘要
        if (StringUtils.isNotBlank(result.getContent())) {
            result.setSummary(generateSummary(result.getContent(), 200));
        }

        return result;
    }

    /**
     * 查找正文元素（多策略融合）
     */
    private Element findContentElement(Document doc) {
        // 策略1：基于语义标签和class选择器
        for (String selector : CONTENT_SELECTORS) {
            Element el = doc.selectFirst(selector);
            if (el != null && isContentLike(el)) {
                log.debug("Found content via selector: {}", selector);
                return el;
            }
        }

        // 策略2：基于文本密度评分
        Element best = findByTextDensity(doc);
        if (best != null) {
            log.debug("Found content via text density scoring");
            return best;
        }

        // 策略3：回退到body
        Element body = doc.body();
        if (body != null) {
            log.debug("Falling back to body element");
            return body;
        }

        return null;
    }

    /**
     * 基于文本密度查找正文元素
     */
    private Element findByTextDensity(Document doc) {
        Element body = doc.body();
        if (body == null) return null;

        Element bestElement = null;
        double bestScore = 0;

        // 遍历所有块级元素，计算文本密度分数
        Elements candidates = body.select("div, section, article, main, td");
        for (Element el : candidates) {
            double score = calculateContentScore(el);
            if (score > bestScore) {
                bestScore = score;
                bestElement = el;
            }
        }

        return bestElement;
    }

    /**
     * 计算元素的内容分数
     */
    private double calculateContentScore(Element el) {
        String text = el.text();
        String html = el.html();

        if (text.length() < minContentLength) {
            return 0;
        }

        // 文本密度
        double textDensity = (double) text.replaceAll("\\s", "").length() / Math.max(html.length(), 1);
        if (textDensity < minTextDensity) {
            return 0;
        }

        // 链接密度（低链接密度=更可能是正文）
        int linkTextLen = 0;
        Elements links = el.select("a");
        for (Element link : links) {
            linkTextLen += link.text().replaceAll("\\s", "").length();
        }
        int totalTextLen = text.replaceAll("\\s", "").length();
        double linkDensity = totalTextLen > 0 ? (double) linkTextLen / totalTextLen : 0;
        if (linkDensity > maxLinkDensity) {
            return 0;
        }

        // 段落分数
        int paragraphs = el.select("p").size();
        double paraScore = Math.log1p(paragraphs);

        // 标签权重
        double tagBonus = getTagBonus(el);

        // 综合分数
        return textDensity * 10 + paraScore + tagBonus - linkDensity * 5;
    }

    /**
     * 判断元素是否像正文
     */
    private boolean isContentLike(Element el) {
        String text = el.text();
        if (text.length() < minContentLength) {
            return false;
        }

        String html = el.html();
        double textDensity = (double) text.replaceAll("\\s", "").length() / Math.max(html.length(), 1);
        if (textDensity < minTextDensity) {
            return false;
        }

        // 检查链接密度
        int linkTextLen = 0;
        for (Element link : el.select("a")) {
            linkTextLen += link.text().replaceAll("\\s", "").length();
        }
        int totalTextLen = text.replaceAll("\\s", "").length();
        double linkDensity = totalTextLen > 0 ? (double) linkTextLen / totalTextLen : 0;

        return linkDensity <= maxLinkDensity;
    }

    /**
     * 获取标签权重加分
     */
    private double getTagBonus(Element el) {
        String tagName = el.tagName().toLowerCase();
        String className = el.className().toLowerCase();
        String id = el.id().toLowerCase();

        double bonus = 0;
        // article标签加分
        if ("article".equals(tagName)) bonus += 3;
        if ("main".equals(tagName)) bonus += 2;
        // class/id包含content/article/post/news加分
        if (className.contains("content") || id.contains("content")) bonus += 2;
        if (className.contains("article") || id.contains("article")) bonus += 2;
        if (className.contains("post") || id.contains("post")) bonus += 1.5;
        if (className.contains("news") || id.contains("news")) bonus += 1.5;
        // sidebar/footer/header减分
        if (className.contains("sidebar") || id.contains("sidebar")) bonus -= 5;
        if (className.contains("footer") || id.contains("footer")) bonus -= 5;
        if (className.contains("header") || id.contains("header")) bonus -= 3;
        if (className.contains("comment") || id.contains("comment")) bonus -= 3;
        if (className.contains("related") || id.contains("related")) bonus -= 2;

        return bonus;
    }

    /**
     * 提取标题
     */
    private String extractTitle(Document doc) {
        // OG title优先
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && StringUtils.isNotBlank(ogTitle.attr("content"))) {
            return ogTitle.attr("content").trim();
        }

        for (String selector : TITLE_SELECTORS) {
            Element el = doc.selectFirst(selector);
            if (el != null && StringUtils.isNotBlank(el.text())) {
                return el.text().trim();
            }
        }

        // 回退到title标签
        Element titleEl = doc.selectFirst("title");
        if (titleEl != null) {
            return titleEl.text().trim();
        }

        return null;
    }

    /**
     * 提取发布日期
     */
    private String extractDate(Document doc) {
        // OG发布时间
        Element ogTime = doc.selectFirst("meta[property=article:published_time]");
        if (ogTime != null && StringUtils.isNotBlank(ogTime.attr("content"))) {
            return ogTime.attr("content").trim();
        }

        for (String selector : DATE_SELECTORS) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                String datetime = el.attr("datetime");
                if (StringUtils.isNotBlank(datetime)) return datetime.trim();
                if (StringUtils.isNotBlank(el.text())) {
                    // 检查是否包含日期格式
                    if (DATE_PATTERN.matcher(el.text()).find()) {
                        return el.text().trim();
                    }
                }
            }
        }

        // 在正文中查找日期
        String bodyText = doc.body() != null ? doc.body().text() : "";
        java.util.regex.Matcher m = DATE_PATTERN.matcher(bodyText);
        if (m.find()) {
            return m.group();
        }

        return null;
    }

    /**
     * 提取作者
     */
    private String extractAuthor(Document doc) {
        Element ogAuthor = doc.selectFirst("meta[property=article:author]");
        if (ogAuthor != null && StringUtils.isNotBlank(ogAuthor.attr("content"))) {
            return ogAuthor.attr("content").trim();
        }

        for (String selector : AUTHOR_SELECTORS) {
            Element el = doc.selectFirst(selector);
            if (el != null && StringUtils.isNotBlank(el.text())) {
                return el.text().trim();
            }
        }

        return null;
    }

    /**
     * 提取标签
     */
    private List<String> extractTags(Document doc) {
        List<String> tags = new ArrayList<>();

        // OG tags
        Elements ogTags = doc.select("meta[property=article:tag]");
        for (Element tag : ogTags) {
            String content = tag.attr("content");
            if (StringUtils.isNotBlank(content)) {
                tags.add(content.trim());
            }
        }

        // Keywords meta
        Element keywords = doc.selectFirst("meta[name=keywords]");
        if (keywords != null && StringUtils.isNotBlank(keywords.attr("content"))) {
            String[] kws = keywords.attr("content").split("[,，;；]");
            for (String kw : kws) {
                if (StringUtils.isNotBlank(kw.trim())) {
                    tags.add(kw.trim());
                }
            }
        }

        return tags;
    }

    /**
     * 提取图片列表
     */
    private List<String> extractImages(Element contentEl, String baseUrl) {
        List<String> images = new ArrayList<>();
        Elements imgs = contentEl.select("img[src]");
        for (Element img : imgs) {
            String src = img.absUrl("src");
            if (StringUtils.isBlank(src)) {
                src = img.attr("src");
                if (StringUtils.isNotBlank(src) && !src.startsWith("data:")) {
                    src = resolveUrl(baseUrl, src);
                }
            }
            if (StringUtils.isNotBlank(src) && !src.startsWith("data:") && !isTrackingPixel(img)) {
                images.add(src);
            }
        }
        return images;
    }

    /**
     * 检测是否为跟踪像素
     */
    private boolean isTrackingPixel(Element img) {
        String width = img.attr("width");
        String height = img.attr("height");
        return ("1".equals(width) && "1".equals(height)) ||
                img.hasClass("tracking") || img.hasClass("pixel");
    }

    /**
     * 清理噪声元素
     */
    private void cleanNoise(Element el) {
        for (String tag : NOISE_TAGS) {
            el.select(tag).remove();
        }
        // 移除广告
        el.select("[class*=ad-], [class*=advert], [id*=ad-], [id*=advert], [class*=share], [class*=social]")
                .remove();
        // 移除空段落
        for (Element p : el.select("p")) {
            if (p.text().trim().isEmpty() && p.select("img").isEmpty()) {
                p.remove();
            }
        }
    }

    /**
     * 清洗文本
     */
    private String cleanText(Element el) {
        String text = el.text();
        // 压缩多余空白
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    /**
     * 生成摘要
     */
    private String generateSummary(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        String truncated = content.substring(0, maxLength);
        int lastPeriod = Math.max(
                Math.max(truncated.lastIndexOf('。'), truncated.lastIndexOf('！')),
                Math.max(truncated.lastIndexOf('？'), truncated.lastIndexOf('.'))
        );
        if (lastPeriod > maxLength * 0.5) {
            return truncated.substring(0, lastPeriod + 1);
        }
        return truncated + "...";
    }

    /**
     * 解析相对URL
     */
    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            return new java.net.URL(new java.net.URL(baseUrl), relativeUrl).toString();
        } catch (Exception e) {
            return relativeUrl;
        }
    }
}