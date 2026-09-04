package com.robot.home.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * 敏感信息脱敏工具
 */
public final class SensitiveUtils {

    private SensitiveUtils() {
    }

    /**
     * 手机号脱敏：138****8888
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏：a***@qq.com
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * 日志参数脱敏：抹掉 password / token / 手机号等敏感字段
     */
    public static String maskParams(String params) {
        if (StrUtil.isBlank(params)) {
            return params;
        }
        String result = params;
        String[] sensitive = {"password", "oldPassword", "newPassword", "token", "accessToken", "refreshToken"};
        for (String key : sensitive) {
            result = result.replaceAll("(?i)\"" + key + "\"\\s*:\\s*\"[^\"]*\"", "\"" + key + "\":\"******\"");
        }
        return result.length() > 2000 ? result.substring(0, 2000) + "...(truncated)" : result;
    }
}
