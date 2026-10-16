package com.robot.home.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Normalizes upload modules and restricts them to application-owned directories.
 */
final class FileModulePolicy {

    private static final Set<String> ALLOWED_MODULES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "common", "article", "brand", "company", "community", "video",
            "tutorial", "recommend", "banner", "robot", "admin", "avatar"
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
}
