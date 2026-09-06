package com.robot.home.common.util;

import cn.hutool.crypto.SecureUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 密码工具：优先使用 BCrypt，兼容旧版 SHA-256+PEPPER 哈希
 * 旧哈希格式：64位十六进制（SHA-256）
 * 新哈希格式：$2a$10$...（BCrypt）
 * 登录时若检测到旧格式，验证通过后自动升级为BCrypt哈希
 */
public class PasswordUtil {

    private static final Logger log = LoggerFactory.getLogger(PasswordUtil.class);

    /** 旧版 SHA-256 pepper（仅用于兼容验证，新密码不再使用） */
    private static final String LEGACY_PEPPER = "robot-home@2026#pepper";

    /** BCrypt 计算成本因子（10 = 2^10 轮） */
    private static final int LOG_ROUNDS = 10;

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * 验证密码：自动识别BCrypt或旧版SHA-256哈希
     * @return VerifyResult 包含是否匹配及是否需要升级哈希
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            // BCrypt 哈希
            return BCrypt.checkpw(rawPassword, storedHash);
        } else {
            // 旧版 SHA-256 哈希（兼容迁移）
            String legacyHash = SecureUtil.sha256(LEGACY_PEPPER + rawPassword);
            return legacyHash.equals(storedHash);
        }
    }

    /**
     * 判断哈希是否为旧版SHA-256格式，需要升级
     */
    public static boolean needsUpgrade(String storedHash) {
        return storedHash != null && !storedHash.startsWith("$2a$") && !storedHash.startsWith("$2b$") && !storedHash.startsWith("$2y$");
    }
}