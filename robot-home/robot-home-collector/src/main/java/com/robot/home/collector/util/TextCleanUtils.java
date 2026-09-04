package com.robot.home.collector.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.regex.Pattern;

/**
 * 文本清洗工具
 * HTML标签移除、实体解码、空白压缩、中文标点处理
 */
public class TextCleanUtils {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("[\\s\\u00A0]+");
    private static final Pattern MULTI_NEWLINE_PATTERN = Pattern.compile("\\n{3,}");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_PATTERN = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--[\\s\\S]*?-->");
    private static final Pattern NAV_PATTERN = Pattern.compile("<nav[^>]*>[\\s\\S]*?</nav>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOOTER_PATTERN = Pattern.compile("<footer[^>]*>[\\s\\S]*?</footer>", Pattern.CASE_INSENSITIVE);

    /**
     * 深度清洗HTML，提取纯文本
     * 移除script/style/nav/footer/comment，解码HTML实体，压缩空白
     */
    public static String htmlToText(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }

        String text = html;
        // 移除script和style
        text = SCRIPT_PATTERN.matcher(text).replaceAll(" ");
        text = STYLE_PATTERN.matcher(text).replaceAll(" ");
        // 移除导航和页脚
        text = NAV_PATTERN.matcher(text).replaceAll(" ");
        text = FOOTER_PATTERN.matcher(text).replaceAll(" ");
        // 移除注释
        text = COMMENT_PATTERN.matcher(text).replaceAll(" ");
        // br/p/div → 换行
        text = text.replaceAll("<br[^>]*>", "\n");
        text = text.replaceAll("</p>", "\n");
        text = text.replaceAll("</div>", "\n");
        text = text.replaceAll("</li>", "\n");
        // 移除剩余HTML标签
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        // 解码HTML实体
        text = StringEscapeUtils.unescapeHtml4(text);
        // 压缩空白
        text = MULTI_SPACE_PATTERN.matcher(text).replaceAll(" ");
        text = MULTI_NEWLINE_PATTERN.matcher(text).replaceAll("\n\n");
        return text.trim();
    }

    /**
     * 简单移除HTML标签（保留内容）
     */
    public static String stripHtmlTags(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        String text = HTML_TAG_PATTERN.matcher(html).replaceAll(" ");
        text = StringEscapeUtils.unescapeHtml4(text);
        text = MULTI_SPACE_PATTERN.matcher(text).replaceAll(" ");
        return text.trim();
    }

    /**
     * 清洗标题：移除网站后缀、特殊字符
     */
    public static String cleanTitle(String title) {
        if (StringUtils.isBlank(title)) {
            return "";
        }
        String t = stripHtmlTags(title);
        // 移除常见网站后缀
        t = t.replaceAll("\\s*[-_|–—]\\s*(首页|官网|官方网站|Home|Homepage|Official Site|Official Website).*$", "");
        t = t.replaceAll("\\s+", " ");
        return t.trim();
    }

    /**
     * 清洗URL文本：提取URL
     */
    public static String extractUrl(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        // 简单匹配http/https URL
        java.util.regex.Matcher m = Pattern.compile("https?://[^\\s<>\"']+").matcher(text);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    /**
     * 截断文本到指定长度，保留完整句子
     */
    public static String truncateWithSentence(String text, int maxLength) {
        if (StringUtils.isBlank(text) || text.length() <= maxLength) {
            return text;
        }

        String truncated = text.substring(0, maxLength);
        // 找最后一个句子结束位置
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
     * 计算文本密度（文本长度/HTML长度），用于正文提取
     */
    public static double textDensity(String html, String text) {
        if (StringUtils.isBlank(html) || html.length() == 0) {
            return 0;
        }
        int textLen = StringUtils.isBlank(text) ? 0 : text.replaceAll("\\s", "").length();
        return (double) textLen / html.length();
    }

    /**
     * 计算链接密度（链接文本长度/总文本长度）
     */
    public static double linkDensity(String html, String text) {
        if (StringUtils.isBlank(html) || StringUtils.isBlank(text)) {
            return 0;
        }
        // 提取所有<a>标签中的文本
        Pattern linkPattern = Pattern.compile("<a[^>]*>([\\s\\S]*?)</a>", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = linkPattern.matcher(html);
        int linkTextLen = 0;
        while (m.find()) {
            linkTextLen += stripHtmlTags(m.group(1)).replaceAll("\\s", "").length();
        }
        int totalTextLen = text.replaceAll("\\s", "").length();
        if (totalTextLen == 0) {
            return 0;
        }
        return (double) linkTextLen / totalTextLen;
    }

    /**
     * 移除零宽字符和不可见字符
     */
    public static String removeInvisibleChars(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        return text.replaceAll("[\\u200B-\\u200D\\uFEFF\\u00AD\\u2060\\u2061\\u2062\\u2063\\u2064]", "");
    }

    /**
     * 规范化空白字符
     */
    public static String normalizeWhitespace(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        return text.replaceAll("[\\t ]+", " ")
                .replaceAll("\\n\\s+\\n", "\n\n")
                .trim();
    }
}