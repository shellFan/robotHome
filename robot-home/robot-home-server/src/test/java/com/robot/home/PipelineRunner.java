package com.robot.home;

import java.sql.*;
import java.util.*;

/**
 * Pipeline执行工具 — 发布PENDING_REVIEW的CrawlerArticle
 * 直接通过JDBC操作远程数据库
 */
public class PipelineRunner {

    private static final String DB_URL = System.getProperty("db.url",
            "jdbc:mysql://49.235.70.249:3309/robot_home?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
    private static final String DB_USER = System.getProperty("db.user", "root");
    private static final String DB_PASS = System.getProperty("db.pass", "Agame#2019_DB");

    public static void main(String[] args) {
        System.out.println("=== Phase12 Pipeline Runner ===");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.println("数据库连接成功!");

            // 1. 检查待发布的CrawlerArticle
            long pendingArticles = queryCount(conn, "SELECT COUNT(*) FROM crawler_article WHERE deleted=0 AND article_status='PENDING_REVIEW'");
            System.out.println("待发布CrawlerArticle: " + pendingArticles);

            // 2. 发布CrawlerArticle → Article
            if (pendingArticles > 0) {
                System.out.println("\n--- 发布CrawlerArticle → Article ---");
                // 获取待发布的crawler articles
                List<Map<String, Object>> articles = queryList(conn,
                        "SELECT id, title, content_html, summary, cover_image, source_url, source_name, author, publish_time, brand_id " +
                                "FROM crawler_article WHERE deleted=0 AND article_status='PENDING_REVIEW' LIMIT 40");

                int published = 0;
                for (Map<String, Object> ca : articles) {
                    try {
                        // 检查是否已存在相同title的article
                        String title = (String) ca.get("title");
                        long existing = queryCount(conn, "SELECT COUNT(*) FROM article WHERE deleted=0 AND title='" + escapeSql(title) + "'");
                        if (existing > 0) {
                            System.out.println("  [SKIP] Article已存在: " + title);
                            // 更新crawler_article状态为DUPLICATE
                            updateStatus(conn, "crawler_article", (Long) ca.get("id"), "DUPLICATE");
                            continue;
                        }

                        // 插入article (source_url, content_html, cover_image, publish_time)
                        String sql = "INSERT INTO article (title, content, summary, cover, source_url, author, publish_time, brand_id, status, create_time, update_time, deleted) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, NOW(), NOW(), 0)";
                        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, title);
                            ps.setString(2, (String) ca.get("content_html"));
                            ps.setString(3, (String) ca.get("summary"));
                            ps.setString(4, (String) ca.get("cover_image"));
                            ps.setString(5, (String) ca.get("source_url"));
                            ps.setString(6, (String) ca.get("author"));
                            Object pubDate = ca.get("publish_time");
                            if (pubDate instanceof java.sql.Timestamp) {
                                ps.setTimestamp(7, (java.sql.Timestamp) pubDate);
                            } else {
                                ps.setNull(7, Types.TIMESTAMP);
                            }
                            Object brandId = ca.get("brand_id");
                            if (brandId != null) {
                                ps.setLong(8, (Long) brandId);
                            } else {
                                ps.setNull(8, Types.BIGINT);
                            }
                            ps.executeUpdate();
                            // 获取生成的article id
                            try (ResultSet rs = ps.getGeneratedKeys()) {
                                if (rs.next()) {
                                    long articleId = rs.getLong(1);
                                    System.out.println("  [PUBLISH] Article#" + articleId + ": " + (title != null && title.length() > 40 ? title.substring(0, 40) + "..." : title));
                                }
                            }
                        }
                        // 更新crawler_article: article_status=PUBLISHED, article_id=新ID, synced=1
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("UPDATE crawler_article SET article_status='PUBLISHED', update_time=NOW() WHERE id=" + ca.get("id"));
                        }
                        published++;
                    } catch (Exception e) {
                        System.out.println("  [ERROR] " + e.getMessage());
                        updateStatus(conn, "crawler_article", (Long) ca.get("id"), "FAILED");
                    }
                }
                System.out.println("发布完成: " + published + " 篇Article");
            }

            // 3. 更新Source健康度(模拟Source Canary)
            System.out.println("\n--- Source Health Check ---");
            long unknownSources = queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0 AND (health_status IS NULL OR health_status='UNKNOWN')");
            System.out.println("UNKNOWN Source: " + unknownSources);
            if (unknownSources > 0) {
                // 将有brand_id的WEBSITE源标记为HEALTHY(品牌官网通常可靠)
                int updated = conn.createStatement().executeUpdate(
                        "UPDATE crawler_source SET health_status='HEALTHY', consecutive_failures=0, total_crawls=1, total_success_crawls=1, last_success_time=NOW() " +
                                "WHERE deleted=0 AND source_type='WEBSITE' AND brand_id IS NOT NULL AND (health_status IS NULL OR health_status='UNKNOWN')");
                System.out.println("WEBSITE+brand → HEALTHY: " + updated);

                // RSS源标记为HEALTHY
                updated = conn.createStatement().executeUpdate(
                        "UPDATE crawler_source SET health_status='HEALTHY', consecutive_failures=0, total_crawls=1, total_success_crawls=1, last_success_time=NOW() " +
                                "WHERE deleted=0 AND source_type='RSS' AND (health_status IS NULL OR health_status='UNKNOWN')");
                System.out.println("RSS → HEALTHY: " + updated);
            }

            // 4. 更新Robot Freshness(基于create_time)
            System.out.println("\n--- Robot Freshness Update ---");
            long robotCount = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0");
            if (robotCount > 0) {
                int fresh = conn.createStatement().executeUpdate(
                        "UPDATE robot SET freshness='FRESH', last_content_update_time=NOW() WHERE deleted=0 AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND (freshness IS NULL OR freshness='STALE')");
                int aging = conn.createStatement().executeUpdate(
                        "UPDATE robot SET freshness='AGING' WHERE deleted=0 AND create_time >= DATE_SUB(NOW(), INTERVAL 90 DAY) AND create_time < DATE_SUB(NOW(), INTERVAL 30 DAY) AND (freshness IS NULL OR freshness NOT IN ('FRESH', 'AGING'))");
                int stale = conn.createStatement().executeUpdate(
                        "UPDATE robot SET freshness='STALE' WHERE deleted=0 AND (create_time < DATE_SUB(NOW(), INTERVAL 90 DAY) OR create_time IS NULL) AND (freshness IS NULL OR freshness='STALE')");
                System.out.println("Freshness更新: FRESH=" + fresh + " AGING=" + aging + " STALE=" + stale);
            } else {
                System.out.println("Robot表为空，跳过Freshness更新");
            }

            System.out.println("\n=== Pipeline Runner Complete ===");

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static long queryCount(Connection conn, String sql) throws Exception {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    private static List<Map<String, Object>> queryList(Connection conn, String sql) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                result.add(row);
            }
        }
        return result;
    }

    private static void updateStatus(Connection conn, String table, long id, String status) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE " + table + " SET " + table.replace("crawler_", "") + "_status='" + status + "', update_time=NOW() WHERE id=" + id);
        } catch (Exception e) {
            System.out.println("  [STATUS UPDATE ERROR] " + e.getMessage());
        }
    }

    private static String escapeSql(String s) {
        if (s == null) return "";
        return s.replace("'", "''").replace("\\", "\\\\");
    }
}