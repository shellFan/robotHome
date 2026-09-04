package com.robot.home.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSON 工具：统一处理 VARCHAR/TEXT 中存放的 JSON 数组
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 解析字符串数组（如图片集、标签）
     */
    public static List<String> parseStringList(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            List<String> list = JSONUtil.toList(JSONUtil.parseArray(json), String.class);
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static String writeStringList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JSONUtil.toJsonStr(list);
    }

    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(JSONUtil.parseArray(json), clazz);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
