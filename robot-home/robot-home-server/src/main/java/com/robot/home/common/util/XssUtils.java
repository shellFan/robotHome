package com.robot.home.common.util;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * XSS 防护：对用户输入的富文本 / 评论做基础清洗
 * 说明：富文本正文由后台编辑发布，前台用户内容一律按纯文本清洗后再入库
 */
public final class XssUtils {

    private XssUtils() {
    }

    private static final Pattern SCRIPT_TAG = Pattern.compile("<\\s*(script|iframe|object|embed|link|style)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SINGLE_TAG = Pattern.compile("<\\s*(script|iframe|object|embed|link|style)\\b[^>]*/?\\s*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_ATTR = Pattern.compile("\\son[a-zA-Z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_URL = Pattern.compile("(?i)\\b(href|src|action)\\s*=\\s*([\"']?)\\s*javascript:",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPR = Pattern.compile("(?i)expression\\s*\\(");

    /**
     * 清洗 HTML：移除脚本标签、事件属性与 javascript: 协议
     */
    public static String clean(String html) {
        if (StrUtil.isBlank(html)) {
            return html;
        }
        String result = html;
        result = SCRIPT_TAG.matcher(result).replaceAll("");
        result = SINGLE_TAG.matcher(result).replaceAll("");
        result = EVENT_ATTR.matcher(result).replaceAll("");
        result = JS_URL.matcher(result).replaceAll("$1=$2#");
        result = EXPR.matcher(result).replaceAll("");
        return result;
    }

    /**
     * 纯文本转义：用于评论等不允许 HTML 的字段
     */
    public static String escapeText(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
