package com.robot.home;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Real Data Run验证工具 — BEFORE Snapshot + 数据质量统计
 * 直接通过JDBC查询远程数据库，不依赖HTTP层
 */
public class DataQualityVerification {

    private static final String DB_URL = System.getProperty("db.url",
            "jdbc:mysql://49.235.70.249:3309/robot_home?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
    private static final String DB_USER = System.getProperty("db.user", "root");
    private static final String DB_PASS = System.getProperty("db.pass", "Agame#2019_DB");

    public static void main(String[] args) {
        System.out.println("=== Phase12 Real Data Run — BEFORE Snapshot ===");
        System.out.println("数据库: " + DB_URL);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.println("数据库连接成功!\n");

            // 1. Robot统计
            System.out.println("=== Robot Coverage ===");
            long robotTotal = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0");
            System.out.println("Robot总数: " + robotTotal);

            long withCoverImage = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND cover_image IS NOT NULL AND cover_image != ''");
            System.out.println("有封面图: " + withCoverImage + " (" + pct(withCoverImage, robotTotal) + "%)");

            long withImages = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND images IS NOT NULL AND images != ''");
            System.out.println("有图集: " + withImages + " (" + pct(withImages, robotTotal) + "%)");

            long withParams = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND main_params IS NOT NULL AND main_params != ''");
            System.out.println("有参数: " + withParams + " (" + pct(withParams, robotTotal) + "%)");

            long withSourceUrl = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND source_url IS NOT NULL AND source_url != ''");
            System.out.println("有来源URL: " + withSourceUrl + " (" + pct(withSourceUrl, robotTotal) + "%)");

            long withBrand = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND brand_id IS NOT NULL AND brand_id > 0");
            System.out.println("有品牌: " + withBrand + " (" + pct(withBrand, robotTotal) + "%)");

            long withCategory = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND category_id IS NOT NULL AND category_id > 0");
            System.out.println("有分类: " + withCategory + " (" + pct(withCategory, robotTotal) + "%)");

            // Freshness
            long fresh = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND freshness = 'FRESH'");
            long aging = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND freshness = 'AGING'");
            long stale = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND freshness = 'STALE'");
            System.out.println("新鲜度: FRESH=" + fresh + " AGING=" + aging + " STALE=" + stale);

            // DataSource分布
            System.out.println("\n=== Robot DataSource Distribution ===");
            queryMap(conn, "SELECT IFNULL(data_source, 'UNKNOWN') as ds, COUNT(*) as cnt FROM robot WHERE deleted=0 GROUP BY data_source ORDER BY cnt DESC")
                    .forEach((k, v) -> System.out.println("  " + k + ": " + v));

            // 2. Source Health
            System.out.println("\n=== Source Health ===");
            long sourceTotal = queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0");
            System.out.println("Source总数: " + sourceTotal);
            queryMap(conn, "SELECT IFNULL(health_status, 'UNKNOWN') as hs, COUNT(*) as cnt FROM crawler_source WHERE deleted=0 GROUP BY health_status ORDER BY cnt DESC")
                    .forEach((k, v) -> System.out.println("  " + k + ": " + v));
            queryMap(conn, "SELECT IFNULL(trust_level, 'NORMAL') as tl, COUNT(*) as cnt FROM crawler_source WHERE deleted=0 GROUP BY trust_level ORDER BY cnt DESC")
                    .forEach((k, v) -> System.out.println("  " + k + ": " + v));

            // 3. Brand统计
            System.out.println("\n=== Brand Coverage ===");
            long brandTotal = queryCount(conn, "SELECT COUNT(*) FROM brand WHERE deleted=0");
            long brandWithWebsite = queryCount(conn, "SELECT COUNT(*) FROM brand WHERE deleted=0 AND website IS NOT NULL AND website != ''");
            System.out.println("Brand总数: " + brandTotal + " 有网站: " + brandWithWebsite + " (" + pct(brandWithWebsite, brandTotal) + "%)");

            // 4. Article统计
            System.out.println("\n=== Article Coverage ===");
            long articleTotal = queryCount(conn, "SELECT COUNT(*) FROM article WHERE deleted=0");
            long articleWithCover = queryCount(conn, "SELECT COUNT(*) FROM article WHERE deleted=0 AND cover IS NOT NULL AND cover != ''");
            long articleWithSource = queryCount(conn, "SELECT COUNT(*) FROM article WHERE deleted=0 AND source IS NOT NULL AND source != ''");
            System.out.println("Article总数: " + articleTotal + " 有封面: " + articleWithCover + " (" + pct(articleWithCover, articleTotal) + "%) 有来源: " + articleWithSource + " (" + pct(articleWithSource, articleTotal) + "%)");

            // 5. Crawler Pipeline
            System.out.println("\n=== Crawler Pipeline ===");
            long crawlerProductTotal = queryCount(conn, "SELECT COUNT(*) FROM crawler_product WHERE deleted=0");
            long crawlerArticleTotal = queryCount(conn, "SELECT COUNT(*) FROM crawler_article WHERE deleted=0");
            System.out.println("CrawlerProduct: " + crawlerProductTotal);
            System.out.println("CrawlerArticle: " + crawlerArticleTotal);
            queryMap(conn, "SELECT IFNULL(product_status, 'UNKNOWN') as ps, COUNT(*) as cnt FROM crawler_product WHERE deleted=0 GROUP BY product_status ORDER BY cnt DESC")
                    .forEach((k, v) -> System.out.println("  Product " + k + ": " + v));
            queryMap(conn, "SELECT IFNULL(article_status, 'UNKNOWN') as as2, COUNT(*) as cnt FROM crawler_article WHERE deleted=0 GROUP BY article_status ORDER BY cnt DESC")
                    .forEach((k, v) -> System.out.println("  Article " + k + ": " + v));

            // 6. data_quality_snapshot表验证
            System.out.println("\n=== Data Quality Snapshot ===");
            long snapshotCount = queryCount(conn, "SELECT COUNT(*) FROM data_quality_snapshot");
            System.out.println("快照记录数: " + snapshotCount);

            // 7. 综合评分计算
            System.out.println("\n=== Overall Score ===");
            double robotScore = (pct(withCoverImage, robotTotal) + pct(withImages, robotTotal) + pct(withParams, robotTotal)
                    + pct(withSourceUrl, robotTotal) + pct(withBrand, robotTotal) + pct(withCategory, robotTotal)) / 6.0;
            double sourceScore = sourceTotal > 0 ? queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0 AND health_status='HEALTHY'") * 100.0 / sourceTotal : 0;
            double freshnessScore = robotTotal > 0 ? fresh * 100.0 / robotTotal : 0;
            double coverageScore = (pct(brandWithWebsite, brandTotal) + pct(articleWithCover, articleTotal) + pct(articleWithSource, articleTotal)) / 3.0;
            double overallScore = Math.round((robotScore * 0.4 + sourceScore * 0.2 + freshnessScore * 0.2 + coverageScore * 0.2) * 100.0) / 100.0;
            System.out.println("Robot Coverage Score: " + round2(robotScore) + "%");
            System.out.println("Source Health Score: " + round2(sourceScore) + "%");
            System.out.println("Freshness Score: " + round2(freshnessScore) + "%");
            System.out.println("Brand/Article Coverage Score: " + round2(coverageScore) + "%");
            System.out.println(">>> OVERALL SCORE: " + round2(overallScore) + "% <<<");

            // 8. GAP分析
            System.out.println("\n=== GAP Analysis ===");
            System.out.println("A5: Robot coverImage NULL = " + (robotTotal - withCoverImage) + "/" + robotTotal + " (待真实采集填充)");
            System.out.println("A6: Source health UNKNOWN = " + queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0 AND (health_status IS NULL OR health_status='UNKNOWN')") + "/" + sourceTotal);
            System.out.println("A8: CSS选择器配置 = 已补齐(GenericWebsiteAdapter.safeSelect)");

            System.out.println("\n=== BEFORE Snapshot Complete ===");

        } catch (Exception e) {
            System.err.println("数据库连接失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static long queryCount(Connection conn, String sql) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    private static Map<String, Long> queryMap(Connection conn, String sql) throws Exception {
        Map<String, Long> result = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getLong(2));
            }
        }
        return result;
    }

    private static double pct(long part, long total) {
        return total > 0 ? Math.round(part * 100.0 / total * 100.0) / 100.0 : 0.0;
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}