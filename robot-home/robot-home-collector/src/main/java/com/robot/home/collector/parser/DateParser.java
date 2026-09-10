package com.robot.home.collector.parser;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用日期解析器
 * 支持：ISO 8601、RFC 822、中文日期、Unix时间戳、相对时间等
 */
@Component
public class DateParser {

    private static final Logger log = LoggerFactory.getLogger(DateParser.class);

    /** 中文数字 */
    private static final String[] CN_NUMBERS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    /** 中文相对时间正则 */
    private static final Pattern CN_RELATIVE_PATTERN = Pattern.compile(
        "(\\d+|十|十几|几十)?\\s*(秒|分钟|小时|天|周|月|年)\\s*(前|后|以前|以后|内)"
    );

    /** Unix时间戳正则（10位秒或13位毫秒） */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{10,13}$");

    /** 日期格式列表（按优先级排序） */
    private static final String[] DATE_PATTERNS = {
        // ISO 8601 变体
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",

        // RFC 822 变体 (RSS常用)
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss",
        "EEE dd MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss",

        // 中文日期格式
        "yyyy年MM月dd日 HH:mm:ss",
        "yyyy年MM月dd日 HH:mm",
        "yyyy年MM月dd日",
        "yyyy年M月d日 HH:mm:ss",
        "yyyy年M月d日 HH:mm",
        "yyyy年M月d日",

        // 常见格式
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy/MM/dd",
        "yyyy-MM-dd",
        "yyyyMMdd",
        "MM/dd/yyyy HH:mm:ss",
        "MM/dd/yyyy",

        // 英文月份格式
        "dd MMM yyyy",
        "MMM dd, yyyy",
        "MMM dd yyyy",
        "MMMM dd, yyyy",
    };

    /**
     * 解析日期字符串
     * @param dateStr 日期字符串
     * @return 解析后的Date，失败返回null
     */
    public Date parse(String dateStr) {
        if (StringUtils.isBlank(dateStr)) return null;

        String trimmed = dateStr.trim();

        // 1. 尝试Unix时间戳
        Date tsResult = parseTimestamp(trimmed);
        if (tsResult != null) return tsResult;

        // 2. 尝试中文相对时间
        Date cnResult = parseChineseRelative(trimmed);
        if (cnResult != null) return cnResult;

        // 3. 尝试英文相对时间 (e.g., "2 hours ago", "3 days ago")
        Date enResult = parseEnglishRelative(trimmed);
        if (enResult != null) return enResult;

        // 4. 标准格式解析
        return parseStandard(trimmed);
    }

    /**
     * 解析日期并格式化为指定格式
     */
    public String parseAndFormat(String dateStr, String outputFormat) {
        Date date = parse(dateStr);
        if (date == null) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(outputFormat);
            return sdf.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析日期并格式化为默认格式 yyyy-MM-dd HH:mm:ss
     */
    public String parseAndFormat(String dateStr) {
        return parseAndFormat(dateStr, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 解析Unix时间戳
     */
    private Date parseTimestamp(String dateStr) {
        Matcher m = TIMESTAMP_PATTERN.matcher(dateStr);
        if (m.matches()) {
            try {
                long ts = Long.parseLong(dateStr);
                // 13位=毫秒，10位=秒
                if (dateStr.length() == 13) {
                    return new Date(ts);
                } else if (dateStr.length() == 10) {
                    return new Date(ts * 1000);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * 解析中文相对时间
     * 支持：3天前、2小时前、1年前、十几分钟前、几十天前
     */
    private Date parseChineseRelative(String dateStr) {
        Matcher m = CN_RELATIVE_PATTERN.matcher(dateStr);
        if (!m.find()) return null;

        String numStr = m.group(1);
        String unit = m.group(2);
        String direction = m.group(3);

        int amount = parseChineseNumber(numStr);
        if (amount <= 0) amount = 1; // 默认1

        long now = System.currentTimeMillis();
        long deltaMs = toMillis(amount, unit);

        // "前/以前" = 过去，"后/以后" = 未来，"内" = 未来
        if (direction.contains("前")) {
            return new Date(now - deltaMs);
        } else {
            return new Date(now + deltaMs);
        }
    }

    /**
     * 解析英文相对时间
     * 支持：2 hours ago, 3 days ago, just now, yesterday
     */
    private Date parseEnglishRelative(String dateStr) {
        String lower = dateStr.toLowerCase(Locale.ENGLISH);

        // just now
        if (lower.equals("just now") || lower.equals("刚刚")) {
            return new Date();
        }

        // yesterday
        if (lower.equals("yesterday")) {
            return new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        }

        // today
        if (lower.equals("today")) {
            return new Date();
        }

        // X units ago
        Pattern agoPattern = Pattern.compile("(\\d+)\\s*(second|minute|hour|day|week|month|year)s?\\s*ago");
        Matcher m = agoPattern.matcher(lower);
        if (m.find()) {
            try {
                int amount = Integer.parseInt(m.group(1));
                String unit = m.group(2);
                long deltaMs = toMillis(amount, unit);
                return new Date(System.currentTimeMillis() - deltaMs);
            } catch (NumberFormatException ignored) {
            }
        }

        // X units from now
        Pattern fromNowPattern = Pattern.compile("(\\d+)\\s*(second|minute|hour|day|week|month|year)s?\\s*from\\s*now");
        m = fromNowPattern.matcher(lower);
        if (m.find()) {
            try {
                int amount = Integer.parseInt(m.group(1));
                String unit = m.group(2);
                long deltaMs = toMillis(amount, unit);
                return new Date(System.currentTimeMillis() + deltaMs);
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    /**
     * 标准格式解析
     */
    private Date parseStandard(String dateStr) {
        // 清理常见前缀/后缀
        String cleaned = dateStr
            .replaceAll("^(Published|Posted|Updated|Date|时间|日期|发布)\\s*[:：]\\s*", "")
            .replaceAll("\\s+(UTC|GMT|CST|EST|PST)$", "")
            .trim();

        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
                sdf.setLenient(false);
                return sdf.parse(cleaned);
            } catch (ParseException ignored) {
                // Try next pattern
            }
        }

        // 宽松模式再试一次
        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
                sdf.setLenient(true);
                return sdf.parse(cleaned);
            } catch (ParseException ignored) {
            }
        }

        log.debug("Could not parse date: {}", dateStr);
        return null;
    }

    /**
     * 解析中文数字
     */
    private int parseChineseNumber(String numStr) {
        if (numStr == null || numStr.isEmpty()) return 0;

        // 纯数字
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException ignored) {
        }

        // "十" = 10
        if (numStr.equals("十")) return 10;
        // "十几" = 10~19
        if (numStr.equals("十几")) return 15;
        // "几十" = 10~90
        if (numStr.equals("几十")) return 50;

        // 中文数字
        int result = 0;
        for (char c : numStr.toCharArray()) {
            for (int i = 0; i < CN_NUMBERS.length; i++) {
                if (CN_NUMBERS[i].equals(String.valueOf(c))) {
                    result = result * 10 + i;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 将数量+单位转为毫秒
     */
    private long toMillis(int amount, String unit) {
        String lower = unit.toLowerCase();
        if (lower.contains("秒") || lower.startsWith("second")) {
            return TimeUnit.SECONDS.toMillis(amount);
        } else if (lower.contains("分钟") || lower.startsWith("minute")) {
            return TimeUnit.MINUTES.toMillis(amount);
        } else if (lower.contains("小时") || lower.startsWith("hour")) {
            return TimeUnit.HOURS.toMillis(amount);
        } else if (lower.contains("天") || lower.startsWith("day")) {
            return TimeUnit.DAYS.toMillis(amount);
        } else if (lower.contains("周") || lower.startsWith("week")) {
            return TimeUnit.DAYS.toMillis(amount * 7L);
        } else if (lower.contains("月") || lower.startsWith("month")) {
            return TimeUnit.DAYS.toMillis(amount * 30L);
        } else if (lower.contains("年") || lower.startsWith("year")) {
            return TimeUnit.DAYS.toMillis(amount * 365L);
        }
        return 0;
    }
}