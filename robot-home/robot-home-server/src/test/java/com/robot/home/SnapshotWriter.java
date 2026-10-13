package com.robot.home;

import java.sql.*;

/**
 * Data Quality Snapshot写入工具 — 将BEFORE/AFTER快照写入data_quality_snapshot表
 */
public class SnapshotWriter {

    private static final String DB_URL = System.getProperty("db.url",
            "jdbc:mysql://49.235.70.249:3309/robot_home?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
    private static final String DB_USER = System.getProperty("db.user", "root");
    private static final String DB_PASS = System.getProperty("db.pass", "Agame#2019_DB");

    public static void main(String[] args) {
        String label = args.length > 0 ? args[0] : "AFTER";
        System.out.println("=== Phase12 Snapshot Writer — " + label + " ===");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.println("数据库连接成功!");

            // Robot Coverage
            long robotTotal = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0");
            long robotWithCover = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND cover_image IS NOT NULL AND cover_image != ''");
            long robotWithGallery = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND images IS NOT NULL AND images != ''");
            long robotWithParams = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND main_params IS NOT NULL AND main_params != ''");
            long robotWithSourceUrl = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND source_url IS NOT NULL AND source_url != ''");
            long robotWithBrand = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND brand_id IS NOT NULL AND brand_id > 0");
            long robotWithCategory = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND category_id IS NOT NULL AND category_id > 0");

            // Freshness
            long robotFresh = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND freshness='FRESH'");
            long robotAging = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND freshness='AGING'");
            long robotStale = queryCount(conn, "SELECT COUNT(*) FROM robot WHERE deleted=0 AND freshness='STALE'");

            // Source Health
            long sourceTotal = queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0");
            long sourceHealthy = queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0 AND health_status='HEALTHY'");
            long sourceDegraded = queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0 AND health_status='DEGRADED'");
            long sourceFailed = queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0 AND health_status='FAILED'");
            long sourceUnknown = queryCount(conn, "SELECT COUNT(*) FROM crawler_source WHERE deleted=0 AND (health_status IS NULL OR health_status='UNKNOWN')");

            // Brand & Article
            long brandTotal = queryCount(conn, "SELECT COUNT(*) FROM brand WHERE deleted=0");
            long brandWithWebsite = queryCount(conn, "SELECT COUNT(*) FROM brand WHERE deleted=0 AND website IS NOT NULL AND website != ''");
            long articleTotal = queryCount(conn, "SELECT COUNT(*) FROM article WHERE deleted=0");
            long articleWithCover = queryCount(conn, "SELECT COUNT(*) FROM article WHERE deleted=0 AND cover IS NOT NULL AND cover != ''");
            long articleWithSource = queryCount(conn, "SELECT COUNT(*) FROM article WHERE deleted=0 AND source_url IS NOT NULL AND source_url != ''");

            // Crawler Pipeline
            long crawlerProductTotal = queryCount(conn, "SELECT COUNT(*) FROM crawler_product WHERE deleted=0");
            long crawlerProductPending = queryCount(conn, "SELECT COUNT(*) FROM crawler_product WHERE deleted=0 AND product_status='PENDING_REVIEW'");
            long crawlerProductPublished = queryCount(conn, "SELECT COUNT(*) FROM crawler_product WHERE deleted=0 AND product_status='PUBLISHED'");
            long crawlerArticleTotal = queryCount(conn, "SELECT COUNT(*) FROM crawler_article WHERE deleted=0");
            long crawlerArticlePending = queryCount(conn, "SELECT COUNT(*) FROM crawler_article WHERE deleted=0 AND article_status='PENDING_REVIEW'");
            long crawlerArticlePublished = queryCount(conn, "SELECT COUNT(*) FROM crawler_article WHERE deleted=0 AND article_status='PUBLISHED'");

            // 计算分数
            double coverRate = robotTotal > 0 ? robotWithCover * 100.0 / robotTotal : 0;
            double galleryRate = robotTotal > 0 ? robotWithGallery * 100.0 / robotTotal : 0;
            double paramRate = robotTotal > 0 ? robotWithParams * 100.0 / robotTotal : 0;
            double sourceUrlRate = robotTotal > 0 ? robotWithSourceUrl * 100.0 / robotTotal : 0;
            double brandRate = robotTotal > 0 ? robotWithBrand * 100.0 / robotTotal : 0;
            double categoryRate = robotTotal > 0 ? robotWithCategory * 100.0 / robotTotal : 0;
            double robotScore = (coverRate + galleryRate + paramRate + sourceUrlRate + brandRate + categoryRate) / 6.0;
            double sourceScore = sourceTotal > 0 ? sourceHealthy * 100.0 / sourceTotal : 0;
            double freshnessScore = robotTotal > 0 ? robotFresh * 100.0 / robotTotal : 0;
            double brandWebsiteRate = brandTotal > 0 ? brandWithWebsite * 100.0 / brandTotal : 0;
            double articleCoverRate = articleTotal > 0 ? articleWithCover * 100.0 / articleTotal : 0;
            double articleSourceRate = articleTotal > 0 ? articleWithSource * 100.0 / articleTotal : 0;
            double coverageScore = (brandWebsiteRate + articleCoverRate + articleSourceRate) / 3.0;
            double overallScore = robotScore * 0.4 + sourceScore * 0.2 + freshnessScore * 0.2 + coverageScore * 0.2;

            // 写入snapshot (使用实际schema的列名)
            String sql = "INSERT INTO data_quality_snapshot " +
                    "(label, snapshot_time, environment, " +
                    "robot_total, robot_cover_image_rate, robot_gallery_rate, robot_param_rate, robot_source_url_rate, robot_brand_rate, robot_category_rate, " +
                    "source_total, source_healthy, source_degraded, source_failed, source_unknown, " +
                    "robot_fresh, robot_aging, robot_stale, " +
                    "brand_total, brand_website_rate, article_total, article_cover_image_rate, article_source_url_rate, " +
                    "crawler_product_total, crawler_product_pending_review, crawler_product_published, " +
                    "crawler_article_total, crawler_article_pending_review, crawler_article_published, " +
                    "overall_score, create_time) " +
                    "VALUES (?, NOW(), 'STAGING', " +
                    "?, ?, ?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, " +
                    "?, ?, ?, " +
                    "?, NOW())";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, label);
                ps.setLong(i++, robotTotal);
                ps.setDouble(i++, round2(coverRate));
                ps.setDouble(i++, round2(galleryRate));
                ps.setDouble(i++, round2(paramRate));
                ps.setDouble(i++, round2(sourceUrlRate));
                ps.setDouble(i++, round2(brandRate));
                ps.setDouble(i++, round2(categoryRate));
                ps.setLong(i++, sourceTotal);
                ps.setLong(i++, sourceHealthy);
                ps.setLong(i++, sourceDegraded);
                ps.setLong(i++, sourceFailed);
                ps.setLong(i++, sourceUnknown);
                ps.setLong(i++, robotFresh);
                ps.setLong(i++, robotAging);
                ps.setLong(i++, robotStale);
                ps.setLong(i++, brandTotal);
                ps.setDouble(i++, round2(brandWebsiteRate));
                ps.setLong(i++, articleTotal);
                ps.setDouble(i++, round2(articleCoverRate));
                ps.setDouble(i++, round2(articleSourceRate));
                ps.setLong(i++, crawlerProductTotal);
                ps.setLong(i++, crawlerProductPending);
                ps.setLong(i++, crawlerProductPublished);
                ps.setLong(i++, crawlerArticleTotal);
                ps.setLong(i++, crawlerArticlePending);
                ps.setLong(i++, crawlerArticlePublished);
                ps.setDouble(i++, round2(overallScore));
                ps.executeUpdate();
            }

            System.out.println("\n=== " + label + " Snapshot 已写入 ===");
            System.out.println("Robot: " + robotTotal + " (cover=" + robotWithCover + ", gallery=" + robotWithGallery + ", params=" + robotWithParams + ", sourceUrl=" + robotWithSourceUrl + ")");
            System.out.println("Source: " + sourceTotal + " (healthy=" + sourceHealthy + ", degraded=" + sourceDegraded + ", failed=" + sourceFailed + ", unknown=" + sourceUnknown + ")");
            System.out.println("Brand: " + brandTotal + " (website=" + brandWithWebsite + ")");
            System.out.println("Article: " + articleTotal + " (cover=" + articleWithCover + ", source=" + articleWithSource + ")");
            System.out.printf("Scores: Robot=%.2f%% Source=%.2f%% Freshness=%.2f%% Coverage=%.2f%%%n", robotScore, sourceScore, freshnessScore, coverageScore);
            System.out.printf(">>> OVERALL: %.2f%% <<<%n", overallScore);

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static long queryCount(Connection conn, String sql) throws Exception {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }
}