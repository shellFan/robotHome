package com.robot.home.collector.adapter;

import com.robot.home.collector.fetcher.FetchResult;
import com.robot.home.collector.util.TextCleanUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信公众号文章采集适配器
 * 适配微信文章页面(w.weixin.qq.com/s/xxx 或 mp.weixin.qq.com/s/xxx)
 */
@Component
public class WeChatAdapter implements CrawlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WeChatAdapter.class);

    private static final Pattern WECHAT_URL_PATTERN = Pattern.compile(
            "^(https?://)?(mp\\.weixin\\.qq\\.com|w\\.weixin\\.qq\\.com)/.*");

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy\u5e74MM\u6708dd\u65e5 HH:mm"),
            DateTimeFormatter.ofPattern("yyyy\u5e74MM\u6708dd\u65e5")
    };

    @Override
    public String getType() {
        return "WECHAT";
    }

    @Override
    public boolean supports(String url, Map<String, String> config) {
        return WECHAT_URL_PATTERN.matcher(url).matches();
    }

    @Override
    public List<String> discoverUrls(String seedUrl, Map<String, String> config) {
        // 微信文章通常不通过种子URL发现，而是直接指定文章列表
        if (seedUrl != null && !seedUrl.isEmpty()) {
            return Arrays.asList(seedUrl.split("\\s*[,\\n]\\s*"));
        }
        return new ArrayList<>();
    }

    @Override
    public ParsedData parse(FetchResult fetchResult, Map<String, String> config) {
        ParsedData data = new ParsedData();
        data.setSourceUrl(fetchResult.getUrl());
        data.setRawHtml(fetchResult.getHtml());

        try {
            Document doc = Jsoup.parse(fetchResult.getHtml(), fetchResult.getUrl());

            // 提取微信文章标题
            String title = extractTitle(doc);
            data.setTitle(title);

            // 提取发布日期
            LocalDateTime publishTime = extractPublishTime(doc);
            if (publishTime != null) {
                data.setPublishDate(publishTime.toString());
            }

            // 提取作者/公众号名称
            String author = extractAuthor(doc);
            data.setAuthor(author);

            // 提取正文内容（微信正文在 #js_content 中）
            String contentHtml = extractContent(doc);
            data.setContentHtml(contentHtml);
            data.setContent(TextCleanUtils.htmlToText(Jsoup.parse(contentHtml).text()));

            // 提取摘要
            data.setSummary(generateSummary(data.getContent()));

            // 提取封面图
            String coverImage = extractCoverImage(doc, fetchResult.getUrl());
            if (coverImage != null && !coverImage.isEmpty()) {
                data.addImage(coverImage);
                data.addMetadata("coverImage", coverImage);
            }

            // 提取文章中的图片
            List<String> images = extractImages(doc, fetchResult.getUrl());
            data.setImages(images);

            // 标记为文章类型
            data.setType("ARTICLE");

            log.info("Parsed WeChat article: title={}, author={}, url={}",
                    title, author, fetchResult.getUrl());

        } catch (Exception e) {
            log.error("Failed to parse WeChat article: {}", fetchResult.getUrl(), e);
            data.setError("Parse error: " + e.getMessage());
        }

        return data;
    }

    @Override
    public List<String> extractLinks(FetchResult fetchResult, Map<String, String> config) {
        // 微信文章页面通常不提取后续链接（单篇文章）
        return new ArrayList<>();
    }

    @Override
    public boolean shouldFollow(String url, int currentDepth, int maxDepth, Map<String, String> config) {
        // 微信文章不跟踪外部链接
        return false;
    }

    // ==================== 私有提取方法 ====================

    private String extractTitle(Document doc) {
        // 尝试多种选择器
        Element titleElem = doc.selectFirst("h1.rich_media_title");
        if (titleElem != null) {
            return titleElem.text().trim();
        }

        titleElem = doc.selectFirst("#activity_name");
        if (titleElem != null) {
            return titleElem.text().trim();
        }

        // 回退到页面标题
        return doc.title();
    }

    private LocalDateTime extractPublishTime(Document doc) {
        // 微信文章时间格式
        Element timeElem = doc.selectFirst("#publish_time");
        if (timeElem != null) {
            return parseDateTime(timeElem.text().trim());
        }

        // 通常在js变量中，尝试从脚本提取
        Elements scripts = doc.select("script");
        for (Element script : scripts) {
            String scriptText = script.html();
            Matcher matcher = Pattern.compile("s\":\"(\\d{4}-\\d{2}-\\d{2}[\\s\\w:]+)\"").matcher(scriptText);
            if (matcher.find()) {
                return parseDateTime(matcher.group(1));
            }
        }

        return null;
    }

    private String extractAuthor(Document doc) {
        // 公众号名称
        Element authorElem = doc.selectFirst("#js_name");
        if (authorElem != null) {
            return authorElem.text().trim();
        }

        // 昵称
        authorElem = doc.selectFirst(".profile_nickname");
        if (authorElem != null) {
            return authorElem.text().trim();
        }

        return null;
    }

    private String extractContent(Document doc) {
        Element contentElem = doc.selectFirst("#js_content");
        if (contentElem != null) {
            return contentElem.html();
        }

        // 回退
        contentElem = doc.selectFirst(".rich_media_content");
        if (contentElem != null) {
            return contentElem.html();
        }

        return doc.body() != null ? doc.body().html() : "";
    }

    private String extractCoverImage(Document doc, String baseUrl) {
        Element metaImg = doc.selectFirst("meta[property=og:image]");
        if (metaImg != null) {
            return metaImg.attr("content");
        }

        Element imgElem = doc.selectFirst("#js_content img");
        if (imgElem != null) {
            return imgElem.absUrl("src");
        }

        return null;
    }

    private List<String> extractImages(Document doc, String baseUrl) {
        List<String> images = new ArrayList<>();
        Elements imgElements = doc.select("#js_content img");
        for (Element img : imgElements) {
            String src = img.absUrl("data-src");
            if (src.isEmpty()) {
                src = img.absUrl("src");
            }
            if (!src.isEmpty()) {
                images.add(src);
            }
        }
        return images;
    }

    private String generateSummary(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // 取前200字符作为摘要
        int maxLen = 200;
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}