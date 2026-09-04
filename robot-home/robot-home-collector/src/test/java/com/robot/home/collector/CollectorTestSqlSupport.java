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
 */
public final class CollectorTestSqlSupport {

    private CollectorTestSqlSupport() {}

    private static final Pattern INLINE_COMMENT = Pattern.compile("\\s+COMMENT\\s+'[^']*'", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENGINE_CLAUSE = Pattern.compile("\\)\\s*ENGINE[^;]*;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern UNIQUE_KEY = Pattern.compile("UNIQUE\\s+KEY\\s+`(\\w+)`\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_TABLE = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?", Pattern.CASE_INSENSITIVE);

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
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.toUpperCase().startsWith("CREATE DATABASE") || trimmed.toUpperCase().startsWith("USE ")) {
                continue;
            }
            Matcher tableMatcher = CREATE_TABLE.matcher(trimmed);
            if (tableMatcher.find()) {
                currentTable = tableMatcher.group(1);
            }
            if (trimmed.startsWith("KEY ")) {
                continue;
            }
            String s = line;
            Matcher m = UNIQUE_KEY.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String name = (currentTable.isEmpty() ? "" : currentTable + "_") + m.group(1);
                m.appendReplacement(sb, "CONSTRAINT " + name + " UNIQUE (");
            }
            m.appendTail(sb);
            s = sb.toString();
            s = INLINE_COMMENT.matcher(s).replaceAll("");
            s = s.replaceAll("(?i)\\bLONGTEXT\\b", "CLOB");
            s = s.replaceAll("(?i)\\bTEXT\\b", "CLOB");
            s = s.replaceAll("(?i)\\bMEDIUMTEXT\\b", "CLOB");
            s = s.replaceAll("(?i)\\bDATETIME\\b", "TIMESTAMP");
            s = s.replaceAll("(?i)DATE_ADD\\(NOW\\(\\),\\s*INTERVAL\\s+(\\d+)\\s+DAY\\)", "DATEADD('DAY', $1, NOW())");
            out.add(s);
        }
        String result = String.join("\n", out);
        result = ENGINE_CLAUSE.matcher(result).replaceAll(");");
        result = result.replaceAll(",\\s*\\n\\s*\\);", "\n);");
        return result;
    }
}