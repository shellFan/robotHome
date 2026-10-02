package com.robot.home.common.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * XSS 防护工具 — 基于 Jsoup Safelist 白名单机制
 * <p>
 * 正则方案无法防御所有 XSS 向量（如编码绕过、嵌套标签、DOM clobbering等），
 * 必须使用基于白名单的 HTML 清洗器。
 * <p>
 * 两个清洗级别:
 * - clean(): 富文本清洗，保留安全的 HTML 标签和属性
 * - escapeText(): 纯文本转义，用于评论等不允许 HTML 的字段
 */
public final class XssUtils {

    private XssUtils() {
    }

    /** 富文本白名单: 允许常见格式化标签 + 链接 + 图片 + 表格 */
    private static final Safelist RICH_TEXT_SAFELIST = Safelist.relaxed()
            .removeTags("iframe", "object", "embed", "form", "input", "textarea",
                    "select", "button", "meta", "link", "style", "base",
                    "applet", "frame", "frameset")
            .addTags("figure", "figcaption", "details", "summary", "abbr",
                    "mark", "sub", "sup", "ins", "del", "hr", "dl", "dt", "dd")
            .addAttributes(":all", "class", "id")
            .addAttributes("a", "target", "rel", "download")
            .addAttributes("img", "loading", "decoding", "width", "height")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan", "scope")
            .addAttributes("ol", "start", "reversed", "type")
            .addProtocols("a", "href", "http", "https", "ftp", "mailto")
            .addProtocols("img", "src", "http", "https");

    /**
     * 清洗 HTML 富文本：移除危险标签和属性，只保留白名单内的安全内容
     * <p>
     * Jsoup Safelist 机制:
     * - 自动移除所有 on* 事件属性 (onclick, onerror 等)
     * - 自动移除 javascript:/vbscript:/data:text/html 协议
     * - 自动移除不在白名单中的标签和属性
     *
     * @param html 原始 HTML
     * @return 安全的 HTML，可安全写入数据库或渲染
     */
    public static String clean(String html) {
        if (html == null || html.trim().isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, RICH_TEXT_SAFELIST);
    }

    /**
     * 纯文本转义：用于评论等不允许 HTML 的字段
     * 将 HTML 特殊字符转义为实体，防止 XSS 注入
     *
     * @param text 原始文本
     * @return 转义后的安全文本
     */
    public static String escapeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&#39;");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * URL安全过滤：移除危险协议(javascript:/vbscript:/data:text/html)
     * 用于前端href输出前的URL清洗，防止通过sourceUrl等字段注入XSS
     *
     * @param url 原始URL
     * @return 安全的URL，危险协议返回空字符串
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }
        String trimmed = url.trim().toLowerCase();
        // 移除前导空白和不可见字符后检测危险协议
        // javascript: 协议 — 可执行任意JS
        if (trimmed.startsWith("javascript:")) {
            return "";
        }
        // vbscript: 协议 — IE专用，可执行VBScript
        if (trimmed.startsWith("vbscript:")) {
            return "";
        }
        // data:text/html — 可内嵌HTML+JS
        if (trimmed.startsWith("data:") && trimmed.contains("text/html")) {
            return "";
        }
        // 仅允许 http/https/ftp/mailto 协议
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")
                && !trimmed.startsWith("ftp://") && !trimmed.startsWith("mailto:")) {
            return "";
        }
        return url.trim();
    }
}