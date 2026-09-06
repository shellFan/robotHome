package com.robot.home.collector;

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
 * 采集器集成测试 SQL 支持：将生产 MySQL 脚本翻译为 H2 可执行语句
 * <p>
 * 关键翻译规则：
 * 1. 跳过 CREATE DATABASE / USE / DROP TABLE 语句
 * 2. 跳过 ALTER TABLE ADD INDEX/KEY 语句（H2 不支持）
 * 3. 移除 AFTER 子句（H2 不支持）
 * 4. 为索引名添加表名前缀（H2 约束名全局唯一，MySQL 为表级别唯一）
 * 5. 移除 COMMENT / ENGINE 子句
 * 6. 类型翻译：LONGTEXT/MEDIUMTEXT/TEXT→CLOB, DATETIME→TIMESTAMP
 * 7. 前缀索引：source_url(191) → source_url（H2 不支持前缀索引）
 */
public final class CollectorTestSqlSupport {

    private CollectorTestSqlSupport() {}

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_COMMENT = Pattern.compile(
            "\\s+COMMENT\\s+'[^']*'", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENGINE_CLAUSE = Pattern.compile(
            "\\)\\s*ENGINE[^;]*;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern UNIQUE_KEY = Pattern.compile(
            "UNIQUE\\s+KEY\\s+`(\\w+)`\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_INDEX = Pattern.compile(
            "KEY\\s+`(\\w+)`\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_ADD_INDEX = Pattern.compile(
            "ALTER\\s+TABLE\\s+.*\\s+ADD\\s+(?:UNIQUE\\s+)?(?:INDEX|KEY)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AFTER_CLAUSE = Pattern.compile(
            "\\s+AFTER\\s+`\\w+`", Pattern.CASE_INSENSITIVE);
    private static final Pattern PREFIX_INDEX = Pattern.compile(
            "`(\\w+)`\\(\\d+\\)");
    private static final Pattern DROP_TABLE = Pattern.compile(
            "DROP\\s+TABLE\\s+IF\\s+EXISTS", Pattern.CASE_INSENSITIVE);

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

    public static String load(String fileName) throws IOException {
        String raw = new String(Files.readAllBytes(sqlDir().resolve(fileName)), StandardCharsets.UTF_8);
        return translate(raw);
    }

    public static String translate(String sql) {
        List<String> out = new ArrayList<>();
        String currentTable = "";
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();

            // 跳过注释和空行
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }

            // 跳过 CREATE DATABASE / USE
            if (trimmed.toUpperCase().startsWith("CREATE DATABASE") || trimmed.toUpperCase().startsWith("USE ")) {
                continue;
            }

            // 跳过 DROP TABLE IF EXISTS（H2 用 CREATE TABLE IF NOT EXISTS 即可）
            if (DROP_TABLE.matcher(trimmed).find()) {
                continue;
            }

            // 跳过 ALTER TABLE ADD INDEX/KEY（H2 不支持）
            if (ALTER_ADD_INDEX.matcher(trimmed).find()) {
                continue;
            }

            // 提取当前表名（用于索引名前缀）
            Matcher tableMatcher = CREATE_TABLE.matcher(trimmed);
            if (tableMatcher.find()) {
                currentTable = tableMatcher.group(1);
            }

            // 跳过普通 KEY 行（非 UNIQUE KEY，单独一行以 "KEY " 开头）
            if (trimmed.startsWith("KEY ")) {
                continue;
            }

            String s = line;

            // ALTER TABLE ADD COLUMN → ALTER TABLE ADD COLUMN IF NOT EXISTS
            // （H2 支持此语法，避免列已存在时报 Duplicate column 错误）
            if (trimmed.toUpperCase().contains("ADD COLUMN") && trimmed.toUpperCase().startsWith("ALTER TABLE")) {
                if (!trimmed.toUpperCase().contains("IF NOT EXISTS")) {
                    s = s.replaceFirst("(?i)ADD\\s+COLUMN", "ADD COLUMN IF NOT EXISTS");
                }
            }

            // 将 CREATE TABLE 转换为 CREATE TABLE IF NOT EXISTS（H2 数据库可能已存在表）
            // 避免重复添加：SQL文件本身可能已包含 IF NOT EXISTS
            if (s.toUpperCase().startsWith("CREATE TABLE") && !s.toUpperCase().contains("IF NOT EXISTS")) {
                s = s.replaceFirst("(?i)CREATE TABLE", "CREATE TABLE IF NOT EXISTS");
            }

            // UNIQUE KEY → CONSTRAINT + 表名前缀
            Matcher ukMatcher = UNIQUE_KEY.matcher(s);
            StringBuffer ukSb = new StringBuffer();
            while (ukMatcher.find()) {
                String name = (currentTable.isEmpty() ? "" : currentTable + "_") + ukMatcher.group(1);
                ukMatcher.appendReplacement(ukSb, "CONSTRAINT " + name + " UNIQUE (");
            }
            ukMatcher.appendTail(ukSb);
            s = ukSb.toString();

            // 行内 KEY（在 CREATE TABLE 语句内）→ 添加表名前缀
            // 注意：上面已经跳过了单独一行的 "KEY "，这里处理行内 KEY
            Matcher keyMatcher = KEY_INDEX.matcher(s);
            StringBuffer keySb = new StringBuffer();
            while (keyMatcher.find()) {
                String name = (currentTable.isEmpty() ? "" : currentTable + "_") + keyMatcher.group(1);
                keyMatcher.appendReplacement(keySb, "INDEX " + name + " (");
            }
            keyMatcher.appendTail(keySb);
            s = keySb.toString();

            // 移除 AFTER 子句
            s = AFTER_CLAUSE.matcher(s).replaceAll("");

            // 移除 COMMENT
            s = INLINE_COMMENT.matcher(s).replaceAll("");

            // 前缀索引：`source_url`(191) → `source_url`（H2 不支持前缀索引）
            s = PREFIX_INDEX.matcher(s).replaceAll("`$1`");

            // 类型翻译
            s = s.replaceAll("(?i)\\bLONGTEXT\\b", "CLOB");
            s = s.replaceAll("(?i)\\bMEDIUMTEXT\\b", "CLOB");
            s = s.replaceAll("(?i)\\bTEXT\\b", "CLOB");
            s = s.replaceAll("(?i)\\bDATETIME\\b", "TIMESTAMP");
            s = s.replaceAll("(?i)DATE_ADD\\(NOW\\(\\),\\s*INTERVAL\\s+(\\d+)\\s+DAY\\)", "DATEADD('DAY', $1, NOW())");

            out.add(s);
        }

        String result = String.join("\n", out);

        // 移除 ENGINE 子句
        result = ENGINE_CLAUSE.matcher(result).replaceAll(");");

        // 清理尾部逗号 + 闭合括号
        result = result.replaceAll(",\\s*\\n\\s*\\);", "\n);");

        return result;
    }
}