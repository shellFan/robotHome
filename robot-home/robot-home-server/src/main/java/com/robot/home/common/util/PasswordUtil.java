package com.robot.home.common.util;

import cn.hutool.crypto.SecureUtil;

/**
 * 密码工具：使用 SHA-256 + 固定 pepper 进行哈希
 * 注：生产环境建议替换为 BCrypt / PBKDF2，此处为轻量实现
 */
public class PasswordUtil {

    private static final String PEPPER = "robot-home@2026#pepper";

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        return SecureUtil.sha256(PEPPER + rawPassword);
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        return hash(rawPassword).equals(storedHash);
    }
}
