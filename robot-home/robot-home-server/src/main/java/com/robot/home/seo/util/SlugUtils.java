package com.robot.home.seo.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * SEO Slug 生成工具
 * 规则:
 * 1. 纯英文/数字名称: 转为小写连字符格式 (e.g. "Unitree Go2" -> "unitree-go2")
 * 2. 含中文的名称: 使用 type-id 格式 (e.g. "robot-123"), 保证可读且唯一
 * 3. 清除特殊字符, 只保留 a-z0-9-
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    private static final Pattern NON_ASCII = Pattern.compile("[^\\x00-\\x7F]");
    private static final Pattern SLUG_CHARS = Pattern.compile("[^a-z0-9-]");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-{2,}");
    private static final int MAX_SLUG_LENGTH = 100;

    /**
     * 判断字符串是否包含非ASCII字符(如中文)
     */
    public static boolean hasNonAscii(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return NON_ASCII.matcher(input).find();
    }

    /**
     * 将英文名称转为slug格式
     * "Unitree Go2 Pro" -> "unitree-go2-pro"
     */
    public static String slugify(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.toLowerCase()
                .replace('_', '-')
                .replace(' ', '-');
        slug = SLUG_CHARS.matcher(slug).replaceAll("");
        slug = MULTI_HYPHEN.matcher(slug).replaceAll("-");
        // 去掉首尾连字符
        while (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        if (slug.isEmpty()) {
            return null;
        }
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH);
            // 截断后去掉末尾可能残留的连字符
            while (slug.endsWith("-")) {
                slug = slug.substring(0, slug.length() - 1);
            }
        }
        return slug;
    }

    /**
     * 生成slug: 英文名slugify, 中文名用 type-id
     *
     * @param name 实体名称
     * @param type 实体类型前缀 (robot/brand/company/article/category)
     * @param id   实体ID
     * @return slug字符串
     */
    public static String generate(String name, String type, Long id) {
        if (name != null && !name.trim().isEmpty() && !hasNonAscii(name)) {
            String slug = slugify(name);
            if (slug != null && slug.length() >= 2) {
                return slug;
            }
        }
        // 中文名或无法slugify: 使用 type-id
        if (id != null) {
            return type + "-" + id;
        }
        return null;
    }
}