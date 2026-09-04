package com.robot.home.collector.parser;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RSS/Atom解析器
 * 基于Rome库，支持RSS 2.0、RSS 1.0、Atom等格式
 */
public class RssParser {

    private static final Logger log = LoggerFactory.getLogger(RssParser.class);

    /**
     * RSS条目
     */
    public static class RssItem {
        private String title;
        private String link;
        private String description;
        private String content;
        private String author;
        private Date publishedDate;
        private List<String> categories = new ArrayList<>();

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public Date getPublishedDate() { return publishedDate; }
        public void setPublishedDate(Date publishedDate) { this.publishedDate = publishedDate; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
    }

    /**
     * RSS解析结果
     */
    public static class RssResult {
        private String feedTitle;
        private String feedLink;
        private String feedDescription;
        private List<RssItem> items = new ArrayList<>();

        public String getFeedTitle() { return feedTitle; }
        public void setFeedTitle(String feedTitle) { this.feedTitle = feedTitle; }
        public String getFeedLink() { return feedLink; }
        public void setFeedLink(String feedLink) { this.feedLink = feedLink; }
        public String getFeedDescription() { return feedDescription; }
        public void setFeedDescription(String feedDescription) { this.feedDescription = feedDescription; }
        public List<RssItem> getItems() { return items; }
        public void setItems(List<RssItem> items) { this.items = items; }
    }

    /**
     * 解析RSS/Atom XML
     */
    public RssResult parse(String xmlContent) {
        RssResult result = new RssResult();
        if (StringUtils.isBlank(xmlContent)) {
            return result;
        }

        SyndFeedInput input = new SyndFeedInput();
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)))) {
            SyndFeed feed = input.build(reader);

            result.setFeedTitle(feed.getTitle());
            result.setFeedLink(feed.getLink());
            result.setFeedDescription(feed.getDescription());

            for (SyndEntry entry : feed.getEntries()) {
                RssItem item = new RssItem();
                item.setTitle(entry.getTitle());
                item.setLink(entry.getLink());
                item.setAuthor(entry.getAuthor());
                item.setPublishedDate(entry.getPublishedDate());

                // 描述（摘要）
                if (entry.getDescription() != null) {
                    item.setDescription(entry.getDescription().getValue());
                }

                // 正文内容
                if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                    item.setContent(entry.getContents().get(0).getValue());
                }

                // 分类
                if (entry.getCategories() != null) {
                    for (SyndCategory cat : entry.getCategories()) {
                        item.getCategories().add(cat.getName());
                    }
                }

                result.getItems().add(item);
            }

            log.info("Parsed RSS feed: {} - {} items", feed.getTitle(), result.getItems().size());
        } catch (Exception e) {
            log.error("Failed to parse RSS/Atom feed", e);
        }

        return result;
    }
}