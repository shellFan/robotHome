package com.robot.home.collector.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sitemap XML解析器
 * 支持sitemap.xml和sitemap index
 */
@Component
public class SitemapParser {

    private static final Logger log = LoggerFactory.getLogger(SitemapParser.class);

    /**
     * 解析Sitemap结果
     */
    public static class SitemapResult {
        private final List<SitemapUrl> urls = new ArrayList<>();
        private final List<String> sitemapIndexes = new ArrayList<>();

        public List<SitemapUrl> getUrls() { return urls; }
        public List<String> getSitemapIndexes() { return sitemapIndexes; }
        public boolean hasSitemapIndexes() { return !sitemapIndexes.isEmpty(); }
    }

    /**
     * Sitemap URL条目
     */
    public static class SitemapUrl {
        private String loc;
        private String lastmod;
        private String changefreq;
        private Double priority;

        public SitemapUrl(String loc, String lastmod, String changefreq, Double priority) {
            this.loc = loc;
            this.lastmod = lastmod;
            this.changefreq = changefreq;
            this.priority = priority;
        }

        public String getLoc() { return loc; }
        public String getLastmod() { return lastmod; }
        public String getChangefreq() { return changefreq; }
        public Double getPriority() { return priority; }
    }

    /**
     * 解析sitemap XML
     */
    public SitemapResult parse(String xmlContent) {
        SitemapResult result = new SitemapResult();
        if (StringUtils.isBlank(xmlContent)) {
            return result;
        }

        try {
            Document doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser());

            // 检查是否为sitemap index
            Elements sitemapElements = doc.select("sitemapindex > sitemap");
            if (!sitemapElements.isEmpty()) {
                for (Element sitemap : sitemapElements) {
                    String loc = text(sitemap, "loc");
                    if (StringUtils.isNotBlank(loc)) {
                        result.getSitemapIndexes().add(loc.trim());
                    }
                }
                return result;
            }

            // 解析普通sitemap
            Elements urlElements = doc.select("urlset > url");
            for (Element urlElem : urlElements) {
                String loc = text(urlElem, "loc");
                if (StringUtils.isBlank(loc)) continue;

                String lastmod = text(urlElem, "lastmod");
                String changefreq = text(urlElem, "changefreq");
                String priorityStr = text(urlElem, "priority");
                Double priority = null;
                try {
                    if (StringUtils.isNotBlank(priorityStr)) {
                        priority = Double.parseDouble(priorityStr);
                    }
                } catch (NumberFormatException ignored) {
                }

                result.getUrls().add(new SitemapUrl(loc.trim(), lastmod, changefreq, priority));
            }

            log.info("Parsed sitemap: {} URLs, {} indexes", result.getUrls().size(), result.getSitemapIndexes().size());
        } catch (Exception e) {
            log.error("Failed to parse sitemap XML", e);
        }

        return result;
    }

    /**
     * 从sitemap URL列表中提取纯URL列表
     */
    public List<String> extractUrls(SitemapResult result) {
        List<String> urls = new ArrayList<>();
        for (SitemapUrl su : result.getUrls()) {
            urls.add(su.getLoc());
        }
        return urls;
    }

    /**
     * 获取子元素文本
     */
    private String text(Element parent, String tag) {
        Element e = parent.selectFirst(tag);
        return e != null ? e.text() : null;
    }
}