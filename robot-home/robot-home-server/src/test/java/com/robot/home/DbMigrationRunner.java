package com.robot.home;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 远程数据库迁移工具 — 应用Phase8-12 SQL迁移
 * 用法: mvn exec:java -Dexec.mainClass="com.robot.home.DbMigrationRunner" -Dexec.classpathScope=test
 */
public class DbMigrationRunner {

    private static final String DB_URL = System.getProperty("db.url",
            "jdbc:mysql://49.235.70.249:3309/robot_home?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
    private static final String DB_USER = System.getProperty("db.user", "root");
    private static final String DB_PASS = System.getProperty("db.pass", "Agame#2019_DB");

    private static final String[] MIGRATION_FILES = {
            "05_brand_alias_and_indexes.sql",
            "06_real_brands_companies.sql",
            "07_real_robot_products.sql",
            "08_search_indexes.sql",
            "09_crawler_sources.sql",
            "10_phase6_beta.sql",
            "11_phase7_content_community_growth.sql",
            "12_phase8_community_procurement_growth.sql",
            "13_phase9_product_growth.sql",
            "14_phase10_ecosystem_growth.sql",
            "15_phase12_real_data_growth.sql",
            "16_phase12_data_quality_snapshot.sql"
    };

    public static void main(String[] args) {
        System.out.println("=== 数据库迁移工具 ===");
        System.out.println("目标: " + DB_URL);
        String sqlBasePath = System.getProperty("sql.path", "../sql");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.println("数据库连接成功!");

            try (Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'robot_home'")) {
                if (rs.next()) System.out.println("当前表数量: " + rs.getInt(1));
            }

            List<String> successFiles = new ArrayList<>();
            List<String> skipFiles = new ArrayList<>();
            List<String> failFiles = new ArrayList<>();

            // Phase5遗漏的列修复(远程库Phase1-7但缺ALTER TABLE)
            fixMissingColumns(conn);

            for (String fileName : MIGRATION_FILES) {
                File sqlFile = new File(sqlBasePath, fileName);
                if (!sqlFile.exists()) {
                    System.out.println("[SKIP] " + fileName + " — 文件不存在");
                    skipFiles.add(fileName);
                    continue;
                }
                System.out.println("[EXEC] " + fileName + " ...");
                try {
                    executeSqlFile(conn, sqlFile);
                    successFiles.add(fileName);
                    System.out.println("[OK] " + fileName);
                } catch (Exception e) {
                    System.out.println("[FAIL] " + fileName + ": " + e.getMessage());
                    failFiles.add(fileName + ": " + e.getMessage());
                }
            }

            try (Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'robot_home'")) {
                if (rs.next()) System.out.println("迁移后表数量: " + rs.getInt(1));
            }

            checkColumn(conn, "robot", "data_source");
            checkColumn(conn, "robot", "freshness");
            checkColumn(conn, "robot", "last_content_update_time");
            checkColumn(conn, "crawler_source", "trust_level");
            checkColumn(conn, "crawler_source", "consecutive_failures");
            checkColumn(conn, "crawler_source", "health_status");
            checkTable(conn, "data_quality_snapshot");
            checkTable(conn, "robot_data_source");

            System.out.println("\n=== 迁移摘要 ===");
            System.out.println("成功: " + successFiles.size());
            for (String f : successFiles) System.out.println("  + " + f);
            if (!skipFiles.isEmpty()) {
                System.out.println("跳过: " + skipFiles.size());
                for (String f : skipFiles) System.out.println("  - " + f);
            }
            if (!failFiles.isEmpty()) {
                System.out.println("失败: " + failFiles.size());
                for (String f : failFiles) System.out.println("  x " + f);
            }
        } catch (Exception e) {
            System.err.println("数据库连接失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void executeSqlFile(Connection conn, File sqlFile) throws Exception {
        List<String> statements = parseSqlFile(sqlFile);
        int executed = 0, skipped = 0;
        for (String sql : statements) {
            String trimmed = sql.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
            if (trimmed.toUpperCase().startsWith("USE ")) continue;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(trimmed);
                executed++;
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("already exists") || msg.contains("Duplicate")
                        || msg.contains("Duplicate key") || msg.contains("Duplicate entry"))) {
                    skipped++;
                } else {
                    System.out.println("  [SQL ERROR] " + msg.substring(0, Math.min(120, msg.length())));
                    throw e;
                }
            }
        }
        System.out.println("  执行: " + executed + " 语句, 幂等跳过: " + skipped);
    }

    /**
     * 解析SQL文件，处理DELIMITER $$语法(JDBC不支持)
     * 将存储过程/函数作为完整语句提取
     */
    private static List<String> parseSqlFile(File sqlFile) throws Exception {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDelimiterMode = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(sqlFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // 处理DELIMITER切换
                if (trimmed.equalsIgnoreCase("DELIMITER $$")) {
                    // 如果有未完成的语句，先保存
                    if (current.length() > 0) {
                        String s = current.toString().trim();
                        if (!s.isEmpty() && !s.startsWith("--")) statements.add(s);
                        current.setLength(0);
                    }
                    inDelimiterMode = true;
                    continue;
                }
                if (trimmed.equalsIgnoreCase("DELIMITER ;")) {
                    // 如果有未完成的语句，先保存
                    if (current.length() > 0) {
                        String s = current.toString().trim();
                        if (!s.isEmpty() && !s.startsWith("--")) statements.add(s);
                        current.setLength(0);
                    }
                    inDelimiterMode = false;
                    continue;
                }

                // 跳过空行和注释
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("--")) continue;

                if (inDelimiterMode) {
                    // DELIMITER $$ 模式: $$作为语句分隔符
                    if (trimmed.endsWith("$$")) {
                        // 去掉末尾的$$，完成一个语句
                        current.append(line, 0, line.lastIndexOf("$$")).append("\n");
                        String s = current.toString().trim();
                        if (!s.isEmpty()) statements.add(s);
                        current.setLength(0);
                    } else {
                        current.append(line).append("\n");
                    }
                } else {
                    // 普通模式: ;作为语句分隔符
                    if (trimmed.endsWith(";")) {
                        current.append(line, 0, line.lastIndexOf(";")).append("\n");
                        String s = current.toString().trim();
                        if (!s.isEmpty() && !s.startsWith("--")) statements.add(s);
                        current.setLength(0);
                    } else {
                        current.append(line).append("\n");
                    }
                }
            }
            // 处理最后的语句
            if (current.length() > 0) {
                String s = current.toString().trim();
                if (!s.isEmpty() && !s.startsWith("--")) statements.add(s);
            }
        }
        return statements;
    }

    private static void checkColumn(Connection conn, String table, String column) {
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                             "WHERE TABLE_SCHEMA='robot_home' AND TABLE_NAME='" + table + "' AND COLUMN_NAME='" + column + "'")) {
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("[CHECK] " + table + "." + column + " OK");
            } else {
                System.out.println("[CHECK] " + table + "." + column + " MISSING!");
            }
        } catch (Exception e) {
            System.out.println("[CHECK] " + table + "." + column + " ERROR: " + e.getMessage());
        }
    }

    private static void checkTable(Connection conn, String table) {
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.TABLES " +
                             "WHERE TABLE_SCHEMA='robot_home' AND TABLE_NAME='" + table + "'")) {
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("[CHECK] " + table + " OK");
            } else {
                System.out.println("[CHECK] " + table + " MISSING!");
            }
        } catch (Exception e) {
            System.out.println("[CHECK] " + table + " ERROR: " + e.getMessage());
        }
    }

    /**
     * 修复Phase5遗漏的列(远程库有Phase1-7的表但缺少ALTER TABLE添加的列)
     */
    private static void fixMissingColumns(Connection conn) {
        System.out.println("[FIX] 修复Phase5遗漏列...");
        String[][] columns = {
                {"robot", "data_source", "VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL' AFTER `main_params`"},
                {"robot", "source_url", "VARCHAR(512) DEFAULT NULL COMMENT '来源URL' AFTER `data_source`"},
                {"robot", "source_name", "VARCHAR(128) DEFAULT NULL COMMENT '来源名称' AFTER `source_url`"},
                {"robot", "last_verified_time", "DATETIME DEFAULT NULL COMMENT '最后验证时间' AFTER `source_name`"},
                {"brand", "data_source", "VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL' AFTER `intro`"},
                {"brand", "source_url", "VARCHAR(512) DEFAULT NULL COMMENT '来源URL' AFTER `data_source`"},
                {"brand", "last_verified_time", "DATETIME DEFAULT NULL COMMENT '最后验证时间' AFTER `source_url`"},
                {"company", "data_source", "VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL' AFTER `intro`"},
                {"company", "source_url", "VARCHAR(512) DEFAULT NULL COMMENT '来源URL' AFTER `data_source`"},
                {"company", "last_verified_time", "DATETIME DEFAULT NULL COMMENT '最后验证时间' AFTER `source_url`"},
                {"company", "business_scope", "VARCHAR(512) DEFAULT NULL COMMENT '经营范围' AFTER `data_source`"},
        };
        for (String[] col : columns) {
            addColumnIfNotExists(conn, col[0], col[1], col[2]);
        }
        System.out.println("[FIX] Phase5遗漏列修复完成");
    }

    private static void addColumnIfNotExists(Connection conn, String table, String column, String definition) {
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                             "WHERE TABLE_SCHEMA='robot_home' AND TABLE_NAME='" + table + "' AND COLUMN_NAME='" + column + "'")) {
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("  [SKIP] " + table + "." + column + " 已存在");
                return;
            }
        } catch (Exception e) {
            System.out.println("  [CHECK ERROR] " + table + "." + column + ": " + e.getMessage());
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
            System.out.println("  [ADD] " + table + "." + column + " OK");
        } catch (Exception e) {
            System.out.println("  [ADD ERROR] " + table + "." + column + ": " + e.getMessage());
        }
    }
}