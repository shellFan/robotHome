package com.robot.home.collector.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTML 白名单清洗工具 — 入库前最后一道防线
 * <p>
 * 使用 Jsoup Safelist 机制，只保留安全的富文本标签和属性。
 * 所有来自外部爬取的 HTML 内容必须经过此工具清洗后才能写入数据库。
 * <p>
 * 禁止标签: script, iframe, object, embed, form, input, textarea, select,
 *           button, meta, link, style, base, applet, frame, frameset
 * 禁止属性: on* 事件处理器, formaction, xlink:href
 * 禁止协议: javascript:, vbscript:, data:text/html
 */
public class HtmlSanitizer {

    private static final Logger log = LoggerFactory.getLogger(HtmlSanitizer.class);

    /** 富文本白名单: 允许常见格式化标签 + 链接 + 图片 + 表格 */
    private static final Safelist RICH_TEXT_SAFELIST = Safelist.relaxed()
            // 移除 relaxed 默认允许的 iframe (relaxed 不含 iframe，但显式移除以防升级变更)
            .removeTags("iframe", "object", "embed", "form", "input", "textarea",
                    "select", "button", "meta", "link", "style", "base",
                    "applet", "frame", "frameset")
            // 添加额外允许的标签
            .addTags("figure", "figcaption", "details", "summary", "abbr",
                    "mark", "sub", "sup", "ins", "del", "hr", "dl", "dt", "dd")
            // 添加额外允许的属性
            .addAttributes(":all", "class", "id")
            .addAttributes("a", "target", "rel", "download")
            .addAttributes("img", "loading", "decoding", "width", "height")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan", "scope")
            .addAttributes("ol", "start", "reversed", "type")
            .addAttributes("video", "controls", "width", "height", "poster", "preload")
            .addAttributes("source", "src", "type")
            .addAttributes("audio", "controls", "preload")
            // 强制协议白名单: 只允许 http/https/ftp/mailto
            .addProtocols("a", "href", "http", "https", "ftp", "mailto")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("audio", "src", "http", "https")
            .addProtocols("source", "src", "http", "https");

    static {
        // 全局强制: 禁止 data: 协议 (防止 data:text/html XSS)
        // Jsoup Safelist 已默认禁止 javascript:/vbscript: 协议
    }

    /**
     * 清洗 HTML 富文本，移除危险标签和属性
     *
     * @param html 原始 HTML (来自外部爬取)
     * @return 安全的 HTML，可安全写入数据库
     */
    public static String sanitize(String html) {
        if (html == null || html.trim().isEmpty()) {
            return html;
        }
        try {
            String cleaned = Jsoup.clean(html, RICH_TEXT_SAFELIST);
            // 二次检查: 移除可能残留的 on* 事件属性 (Jsoup Safelist 理论上已移除，但做防御性检查)
            // Jsoup.clean 已经基于白名单机制移除了所有不在白名单中的属性，on* 事件属性不在白名单中
            // 因此这里不需要额外正则处理
            return cleaned;
        } catch (Exception e) {
            log.warn("HTML sanitization failed, returning empty string for safety: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 清洗 HTML 并添加安全增强属性
     * - 为所有 a 标签添加 rel="noopener noreferrer"
     * - 外部链接强制 target="_blank"
     * - img 标签添加 loading="lazy"
     *
     * @param html 原始 HTML
     * @return 安全的 HTML，含安全增强属性
     */
    public static String sanitizeWithEnhancements(String html) {
        String sanitized = sanitize(html);
        if (sanitized == null || sanitized.trim().isEmpty()) {
            return sanitized;
        }
        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(sanitized);
            // 为所有 a 标签添加安全属性
            doc.select("a[href]").forEach(a -> {
                a.attr("rel", "noopener noreferrer");
                String href = a.attr("href");
                if (href.startsWith("http://") || href.startsWith("https://")) {
                    a.attr("target", "_blank");
                }
            });
            // 为所有 img 标签添加 lazy loading
            doc.select("img").forEach(img -> {
                if (!img.hasAttr("loading")) {
                    img.attr("loading", "lazy");
                }
            });
            // 提取 body 内容 (Jsoup.parse 会添加 html/head/body 包装)
            return doc.body().html();
        } catch (Exception e) {
            log.warn("HTML enhancement failed, returning basic sanitized result: {}", e.getMessage());
            return sanitized;
        }
    }
}