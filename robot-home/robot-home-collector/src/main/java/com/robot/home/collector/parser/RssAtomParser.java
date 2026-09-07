package com.robot.home.collector.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RSS/Atom Feed解析器
 * 支持RSS 2.0、RSS 1.0(RDF)、Atom格式
 */
@Component
public class RssAtomParser {

    private static final Logger log = LoggerFactory.getLogger(RssAtomParser.class);

    /** 最大条目数 */
    private static final int MAX_ENTRIES = 1000;

    /**
     * Feed条目
     */
    public static class FeedEntry {
        private String title;
        private String link;
        private String description;
        private String author;
        private Date publishedDate;
        private String publishedDateStr;
        private String guid;
        private List<String> categories = new ArrayList<>();

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public Date getPublishedDate() { return publishedDate; }
        public void setPublishedDate(Date publishedDate) { this.publishedDate = publishedDate; }
        public String getPublishedDateStr() { return publishedDateStr; }
        public void setPublishedDateStr(String publishedDateStr) { this.publishedDateStr = publishedDateStr; }
        public String getGuid() { return guid; }
        public void setGuid(String guid) { this.guid = guid; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
    }

    /**
     * Feed解析结果
     */
    public static class FeedResult {
        private String title;
        private String link;
        private String description;
        private List<FeedEntry> entries = new ArrayList<>();

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<FeedEntry> getEntries() { return entries; }
        public void setEntries(List<FeedEntry> entries) { this.entries = entries; }
    }

    /**
     * 解析Feed内容（自动检测RSS/Atom格式）
     */
    public FeedResult parse(String content) {
        if (StringUtils.isBlank(content)) {
            return new FeedResult();
        }

        try {
            Document doc = Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());

            // 检测格式
            if (isAtomFeed(doc)) {
                return parseAtom(doc);
            } else if (isRss1Feed(doc)) {
                return parseRss1(doc);
            } else {
                return parseRss2(doc);
            }
        } catch (Exception e) {
            log.error("Failed to parse RSS/Atom feed", e);
            return new FeedResult();
        }
    }

    /**
     * 检测是否为Atom格式
     */
    private boolean isAtomFeed(Document doc) {
        return doc.selectFirst("feed") != null ||
               doc.selectFirst("atom\\:feed") != null;
    }

    /**
     * 检测是否为RSS 1.0(RDF)格式
     */
    private boolean isRss1Feed(Document doc) {
        return doc.selectFirst("RDF") != null ||
               doc.selectFirst("rdf\\:RDF") != null;
    }

    /**
     * 解析RSS 2.0格式
     */
    private FeedResult parseRss2(Document doc) {
        FeedResult result = new FeedResult();

        Element channel = doc.selectFirst("channel");
        if (channel != null) {
            result.setTitle(text(channel, "title"));
            result.setLink(text(channel, "link"));
            result.setDescription(text(channel, "description"));
        }

        Elements items = doc.select("item");
        for (Element item : items) {
            if (result.getEntries().size() >= MAX_ENTRIES) break;

            FeedEntry entry = new FeedEntry();
            entry.setTitle(text(item, "title"));
            entry.setLink(text(item, "link"));
            entry.setDescription(text(item, "description"));
            entry.setAuthor(text(item, "author"));
            entry.setGuid(text(item, "guid"));

            // 日期解析
            String pubDate = text(item, "pubDate");
            if (StringUtils.isBlank(pubDate)) {
                pubDate = text(item, "dc\\:date");
            }
            entry.setPublishedDateStr(pubDate);
            entry.setPublishedDate(parseDate(pubDate));

            // 分类
            Elements cats = item.select("category");
            for (Element cat : cats) {
                entry.getCategories().add(cat.text());
            }

            result.getEntries().add(entry);
        }

        log.info("Parsed RSS 2.0 feed: {} entries", result.getEntries().size());
        return result;
    }

    /**
     * 解析RSS 1.0(RDF)格式
     */
    private FeedResult parseRss1(Document doc) {
        FeedResult result = new FeedResult();

        Element channel = doc.selectFirst("channel");
        if (channel != null) {
            result.setTitle(text(channel, "title"));
            result.setLink(text(channel, "link"));
            result.setDescription(text(channel, "description"));
        }

        Elements items = doc.select("item");
        for (Element item : items) {
            if (result.getEntries().size() >= MAX_ENTRIES) break;

            FeedEntry entry = new FeedEntry();
            entry.setTitle(text(item, "title"));
            entry.setLink(item.attr("rdf:about"));
            if (StringUtils.isBlank(entry.getLink())) {
                entry.setLink(text(item, "link"));
            }
            entry.setDescription(text(item, "description"));
            entry.setAuthor(text(item, "dc\\:creator"));

            String dcDate = text(item, "dc\\:date");
            entry.setPublishedDateStr(dcDate);
            entry.setPublishedDate(parseDate(dcDate));

            result.getEntries().add(entry);
        }

        log.info("Parsed RSS 1.0 (RDF) feed: {} entries", result.getEntries().size());
        return result;
    }

    /**
     * 解析Atom格式
     */
    private FeedResult parseAtom(Document doc) {
        FeedResult result = new FeedResult();

        Element feed = doc.selectFirst("feed");
        if (feed == null) {
            feed = doc.selectFirst("atom\\:feed");
        }

        if (feed != null) {
            result.setTitle(text(feed, "title"));
            // Atom link使用href属性
            Element linkElem = feed.selectFirst("link[rel=alternate]");
            if (linkElem == null) linkElem = feed.selectFirst("link");
            if (linkElem != null) {
                result.setLink(linkElem.attr("href"));
            }
            result.setDescription(text(feed, "subtitle"));
        }

        Elements entries = feed != null ? feed.select("entry") : new Elements();
        for (Element entryElem : entries) {
            if (result.getEntries().size() >= MAX_ENTRIES) break;

            FeedEntry entry = new FeedEntry();
            entry.setTitle(text(entryElem, "title"));

            // Atom link
            Element linkEl = entryElem.selectFirst("link[rel=alternate]");
            if (linkEl == null) linkEl = entryElem.selectFirst("link");
            if (linkEl != null) {
                entry.setLink(linkEl.attr("href"));
            }

            // content或summary
            String content = text(entryElem, "content");
            if (StringUtils.isBlank(content)) {
                content = text(entryElem, "summary");
            }
            entry.setDescription(content);

            // 作者
            Element authorElem = entryElem.selectFirst("author > name");
            if (authorElem != null) {
                entry.setAuthor(authorElem.text());
            }

            // 日期
            String updated = text(entryElem, "updated");
            String published = text(entryElem, "published");
            String dateStr = StringUtils.isNotBlank(published) ? published : updated;
            entry.setPublishedDateStr(dateStr);
            entry.setPublishedDate(parseDate(dateStr));

            // ID
            entry.setGuid(text(entryElem, "id"));

            // 分类
            Elements cats = entryElem.select("category");
            for (Element cat : cats) {
                String term = cat.attr("term");
                if (StringUtils.isNotBlank(term)) {
                    entry.getCategories().add(term);
                } else {
                    entry.getCategories().add(cat.text());
                }
            }

            result.getEntries().add(entry);
        }

        log.info("Parsed Atom feed: {} entries", result.getEntries().size());
        return result;
    }

    /**
     * 解析日期字符串
     * 支持多种格式：RFC 822、ISO 8601等
     */
    private Date parseDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) return null;

        // 常见RSS/Atom日期格式
        String[] patterns = {
            "EEE, dd MMM yyyy HH:mm:ss Z",     // RFC 822 (RSS 2.0标准)
            "EEE, dd MMM yyyy HH:mm:ss z",     // RFC 822 variant
            "yyyy-MM-dd'T'HH:mm:ssXXX",         // ISO 8601 with timezone
            "yyyy-MM-dd'T'HH:mm:ssZ",           // ISO 8601 with numeric tz
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",     // ISO 8601 with millis
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",       // ISO 8601 with millis numeric
            "yyyy-MM-dd'T'HH:mm:ss",            // ISO 8601 no timezone
            "yyyy-MM-dd HH:mm:ss",              // Common SQL format
            "yyyy-MM-dd",                        // Date only
            "dd MMM yyyy HH:mm:ss Z",           // Without day name
        };

        String trimmed = dateStr.trim();

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
                return sdf.parse(trimmed);
            } catch (ParseException ignored) {
                // Try next pattern
            }
        }

        log.debug("Could not parse date: {}", dateStr);
        return null;
    }

    /**
     * 从Feed条目提取URL列表
     */
    public List<String> extractUrls(FeedResult result) {
        List<String> urls = new ArrayList<>();
        for (FeedEntry entry : result.getEntries()) {
            if (StringUtils.isNotBlank(entry.getLink())) {
                urls.add(entry.getLink());
            }
        }
        return urls;
    }

    /**
     * 获取子元素文本
     */
    private String text(Element parent, String selector) {
        Element e = parent.selectFirst(selector);
        return e != null ? e.text() : null;
    }
}