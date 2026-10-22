package com.robot.home.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Normalizes upload modules and restricts them to application-owned directories.
 * 同时区分公开模块和私有模块：私有模块文件不得通过公开 /files/ 访问。
 */
final class FileModulePolicy {

    private static final Set<String> ALLOWED_MODULES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "common", "article", "brand", "company", "community", "video",
            "tutorial", "recommend", "banner", "robot", "admin", "avatar",
            "enterprise"
    )));

    /**
     * 私有模块：企业认证材料、营业执照、采购合同等敏感文件。
     * 私有模块文件存储在独立目录，不通过公开 /files/ 暴露，
     * 必须通过带鉴权的 /api/files/private/ 端点访问。
     */
    private static final Set<String> PRIVATE_MODULES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "enterprise"
    )));

    private FileModulePolicy() {
    }

    static String normalize(String module) {
        String normalized = module == null ? "common" : module.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            normalized = "common";
        }
        if (!ALLOWED_MODULES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的文件模块");
        }
        return normalized;
    }

    /**
     * 判断模块是否为私有模块（文件不得公开访问）
     */
    static boolean isPrivateModule(String module) {
        String normalized = module == null ? "common" : module.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            normalized = "common";
        }
        return PRIVATE_MODULES.contains(normalized);
    }
}
