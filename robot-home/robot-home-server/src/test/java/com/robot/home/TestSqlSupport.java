package com.robot.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 集成测试支持：将生产 MySQL 脚本翻译为 H2（MySQL 兼容模式）可执行语句。
 * 仅用于测试环境验证表结构与 SQL 联通，不影响生产脚本本身。
 */
public final class TestSqlSupport {

    private TestSqlSupport() {
    }

    private static final Pattern INLINE_COMMENT = Pattern.compile("\\s+COMMENT\\s+'[^']*'", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENGINE_CLAUSE = Pattern.compile("\\)\\s*ENGINE[^;]*;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern UNIQUE_KEY = Pattern.compile("UNIQUE\\s+KEY\\s+`(\\w+)`\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_TABLE = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?", Pattern.CASE_INSENSITIVE);

    /**
     * 定位 sql 目录（模块上级目录 /sql）
     */
    public static Path sqlDir() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 5; i++) {
            Path candidate = dir.resolve("sql");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("未找到 sql 目录，user.dir=" + System.getProperty("user.dir"));
    }

    /**
     * 读取并翻译脚本
     */
    public static String load(String fileName) throws IOException {
        String raw = new String(Files.readAllBytes(sqlDir().resolve(fileName)), StandardCharsets.UTF_8);
        return translate(raw);
    }

    /**
     * MySQL -> H2 翻译
     */
    public static String translate(String sql) {
        List<String> out = new ArrayList<>();
        // H2 的约束名在库级别唯一（MySQL 为表级别），这里按表名加前缀避免重名
        String currentTable = "";
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.toUpperCase().startsWith("CREATE DATABASE") || trimmed.toUpperCase().startsWith("USE ")) {
                continue;
            }
            // ALTER TABLE ADD INDEX / ADD KEY -> 测试环境跳过（H2 不需要索引验证功能）
            if (trimmed.toUpperCase().matches("ALTER\\s+TABLE.*ADD\\s+(INDEX|KEY).*")) {
                continue;
            }
            Matcher tableMatcher = CREATE_TABLE.matcher(trimmed);
            if (tableMatcher.find()) {
                currentTable = tableMatcher.group(1);
            }
            // 普通索引定义：H2 内联语法不兼容，测试环境直接忽略（不影响功能正确性）
            if (trimmed.startsWith("KEY ")) {
                continue;
            }
            String s = line;
            // ALTER TABLE ADD COLUMN ... AFTER -> 移除 AFTER 子句（H2 不支持）
            s = s.replaceAll("(?i)\\s+AFTER\\s+`?\\w+`?", "");
            // UNIQUE KEY -> CONSTRAINT ... UNIQUE（约束名加表名前缀）
            Matcher m = UNIQUE_KEY.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String name = (currentTable.isEmpty() ? "" : currentTable + "_") + m.group(1);
                m.appendReplacement(sb, "CONSTRAINT " + name + " UNIQUE (");
            }
            m.appendTail(sb);
            s = sb.toString();
            // 字段级 COMMENT 移除
            s = INLINE_COMMENT.matcher(s).replaceAll("");
            // 类型映射
            s = s.replaceAll("(?i)\\bLONGTEXT\\b", "CLOB");
            s = s.replaceAll("(?i)\\bTEXT\\b", "CLOB");
            s = s.replaceAll("(?i)\\bDATETIME\\b", "TIMESTAMP");
            // MySQL 日期函数 -> H2
            s = s.replaceAll("(?i)DATE_ADD\\(NOW\\(\\),\\s*INTERVAL\\s+(\\d+)\\s+DAY\\)", "DATEADD('DAY', $1, NOW())");
            out.add(s);
        }
        String result = String.join("\n", out);
        // 建表尾部 ENGINE / CHARSET / COMMENT
        result = ENGINE_CLAUSE.matcher(result).replaceAll(");");
        // 去掉最后一个字段定义后的多余逗号
        result = result.replaceAll(",\\s*\\n\\s*\\);", "\n);");
        return result;
    }
}
