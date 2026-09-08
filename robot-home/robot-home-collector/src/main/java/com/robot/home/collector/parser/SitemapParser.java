package com.robot.home.collector.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Sitemap XML解析器
 * 支持：sitemap.xml、sitemap index、gzip压缩、多层递归
 * 
 * 安全说明（XXE防护）：
 * 本解析器使用 Jsoup.parse(xml, Parser.xmlParser())，Jsoup的XML解析器
 * 默认不解析外部实体(External Entities)和DTD，因此天然免疫XXE攻击。
 * 不需要像标准Java XML解析器(SAXParserFactory/DocumentBuilderFactory)那样
 * 显式禁用外部实体。如未来切换到标准XML解析器，必须添加XXE防护配置。
 */
@Component
public class SitemapParser {

    private static final Logger log = LoggerFactory.getLogger(SitemapParser.class);

    /** 最大递归深度（防止无限递归） */
    private static final int MAX_RECURSION_DEPTH = 3;

    /** 单个sitemap最大URL数（防止内存溢出） */
    private static final int MAX_URLS_PER_SITEMAP = 50000;

    /** GZIP magic bytes */
    private static final byte[] GZIP_MAGIC = {(byte) 0x1f, (byte) 0x8b};

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
     * 解析sitemap XML（自动检测gzip）
     * @param content sitemap内容（可能是XML文本或gzip字节数组）
     */
    public SitemapResult parse(String content) {
        SitemapResult result = new SitemapResult();
        if (StringUtils.isBlank(content)) {
            return result;
        }

        String xmlContent = content;
        // 检测是否为gzip压缩内容（content可能来自HTTP响应的body bytes转string）
        // 注意：gzip检测在fetcher层处理，这里主要处理纯XML

        parseXmlContent(xmlContent, result);
        return result;
    }

    /**
     * 解析gzip压缩的sitemap
     * @param gzipData gzip压缩的字节数组
     */
    public SitemapResult parseGzip(byte[] gzipData) {
        SitemapResult result = new SitemapResult();
        if (gzipData == null || gzipData.length == 0) {
            return result;
        }

        try {
            String xmlContent = decompressGzip(gzipData);
            parseXmlContent(xmlContent, result);
        } catch (Exception e) {
            log.error("Failed to decompress gzip sitemap", e);
        }

        return result;
    }

    /**
     * 解析字节数组（自动检测gzip）
     * @param data sitemap字节数据
     * @param contentType HTTP Content-Type
     */
    public SitemapResult parseBytes(byte[] data, String contentType) {
        if (data == null || data.length == 0) {
            return new SitemapResult();
        }

        // 检测gzip：通过magic bytes或Content-Type
        boolean isGzip = isGzipData(data) ||
                (contentType != null && contentType.contains("gzip"));

        if (isGzip) {
            return parseGzip(data);
        } else {
            try {
                String xmlContent = new String(data, "UTF-8");
                return parse(xmlContent);
            } catch (Exception e) {
                log.error("Failed to parse sitemap bytes as UTF-8", e);
                return new SitemapResult();
            }
        }
    }

    /**
     * 解析XML内容
     */
    private void parseXmlContent(String xmlContent, SitemapResult result) {
        try {
            Document doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser());

            // 1. 检查是否为sitemap index
            Elements sitemapElements = doc.select("sitemapindex > sitemap");
            if (!sitemapElements.isEmpty()) {
                for (Element sitemap : sitemapElements) {
                    String loc = text(sitemap, "loc");
                    String lastmod = text(sitemap, "lastmod");
                    if (StringUtils.isNotBlank(loc)) {
                        result.getSitemapIndexes().add(loc.trim());
                        log.debug("Found sitemap index entry: {} (lastmod: {})", loc.trim(), lastmod);
                    }
                }
                log.info("Parsed sitemap index: {} sub-sitemaps", result.getSitemapIndexes().size());
                return;
            }

            // 2. 解析普通sitemap urlset
            Elements urlElements = doc.select("urlset > url");
            if (urlElements.isEmpty()) {
                // 尝试不带命名空间前缀的选择器（某些sitemap使用不同命名空间）
                urlElements = doc.select("url");
            }

            for (Element urlElem : urlElements) {
                if (result.getUrls().size() >= MAX_URLS_PER_SITEMAP) {
                    log.warn("Sitemap URL count exceeded max limit {}, truncating", MAX_URLS_PER_SITEMAP);
                    break;
                }

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

            log.info("Parsed sitemap: {} URLs", result.getUrls().size());
        } catch (Exception e) {
            log.error("Failed to parse sitemap XML", e);
        }
    }

    /**
     * 解压gzip数据
     */
    private String decompressGzip(byte[] gzipData) throws Exception {
        try (InputStream is = new ByteArrayInputStream(gzipData);
             GZIPInputStream gis = new GZIPInputStream(is);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return os.toString("UTF-8");
        }
    }

    /**
     * 检测数据是否为gzip格式
     */
    private boolean isGzipData(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        return (data[0] & 0xFF) == (GZIP_MAGIC[0] & 0xFF) &&
               (data[1] & 0xFF) == (GZIP_MAGIC[1] & 0xFF);
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
     * 过滤URL：按changefreq和lastmod筛选最近更新的URL
     * @param urls sitemap URL列表
     * @param maxAgeDays 最大天数（0=不过滤）
     * @return 过滤后的URL列表
     */
    public List<String> filterRecentUrls(List<SitemapUrl> urls, int maxAgeDays) {
        if (maxAgeDays <= 0) {
            return extractUrls(new SitemapResult() {{ getUrls().addAll(urls); }});
        }

        List<String> filtered = new ArrayList<>();
        long cutoffMs = System.currentTimeMillis() - (long) maxAgeDays * 24 * 60 * 60 * 1000;

        for (SitemapUrl su : urls) {
            if (StringUtils.isBlank(su.getLastmod())) {
                filtered.add(su.getLoc());
                continue;
            }
            try {
                // 尝试解析lastmod（ISO 8601格式）
                String lastmod = su.getLastmod().trim();
                // 简单解析 YYYY-MM-DD 格式
                if (lastmod.length() >= 10) {
                    String dateStr = lastmod.substring(0, 10);
                    long lastmodMs = java.sql.Date.valueOf(dateStr).getTime();
                    if (lastmodMs >= cutoffMs) {
                        filtered.add(su.getLoc());
                    }
                } else {
                    filtered.add(su.getLoc());
                }
            } catch (Exception e) {
                // 无法解析日期，保留URL
                filtered.add(su.getLoc());
            }
        }

        log.info("Filtered sitemap URLs: {} -> {} (maxAgeDays={})", urls.size(), filtered.size(), maxAgeDays);
        return filtered;
    }

    /**
     * 按优先级排序URL（priority高的在前）
     */
    public List<SitemapUrl> sortByPriority(List<SitemapUrl> urls) {
        List<SitemapUrl> sorted = new ArrayList<>(urls);
        sorted.sort((a, b) -> {
            double pa = a.getPriority() != null ? a.getPriority() : 0.5;
            double pb = b.getPriority() != null ? b.getPriority() : 0.5;
            return Double.compare(pb, pa); // 降序
        });
        return sorted;
    }

    /**
     * 获取子元素文本
     */
    private String text(Element parent, String tag) {
        Element e = parent.selectFirst(tag);
        return e != null ? e.text() : null;
    }
}