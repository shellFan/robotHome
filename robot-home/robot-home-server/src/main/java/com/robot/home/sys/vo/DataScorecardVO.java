package com.robot.home.sys.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据Scorecard VO — Phase12 P0-6: BEFORE/AFTER对比快照
 */
@Data
public class DataScorecardVO {

    /** 快照时间 */
    private LocalDateTime snapshotTime;

    /** 快照标签(BEFORE/AFTER) */
    private String label;

    // ===== Robot Coverage =====
    private Long robotTotal;
    private Double robotCoverImageRate;
    private Double robotGalleryRate;
    private Double robotParamRate;
    private Double robotSourceUrlRate;
    private Double robotBrandRate;
    private Double robotCategoryRate;

    // ===== Source Health =====
    private Long sourceTotal;
    private Long sourceHealthy;
    private Long sourceDegraded;
    private Long sourceFailed;
    private Long sourceUnknown;

    // ===== Freshness =====
    private Long robotFresh;
    private Long robotAging;
    private Long robotStale;

    // ===== Brand & Article =====
    private Long brandTotal;
    private Double brandWebsiteRate;
    private Long articleTotal;
    private Double articleCoverImageRate;
    private Double articleSourceUrlRate;

    // ===== Crawler Pipeline =====
    private Long crawlerProductTotal;
    private Long crawlerProductPendingReview;
    private Long crawlerProductPublished;
    private Long crawlerArticleTotal;
    private Long crawlerArticlePendingReview;
    private Long crawlerArticlePublished;

    // ===== 综合评分(0-100) =====
    private Double overallScore;

    /**
     * 计算综合评分: 加权平均
     * Robot覆盖率(40%) + Source健康度(20%) + Freshness(20%) + Brand/Article覆盖(20%)
     */
    public void computeOverallScore() {
        double robotScore = (robotCoverImageRate + robotGalleryRate + robotParamRate
                + robotSourceUrlRate + robotBrandRate + robotCategoryRate) / 6.0;
        double sourceScore = sourceTotal > 0
                ? (sourceHealthy * 100.0 / sourceTotal) : 0.0;
        double freshnessScore = robotTotal > 0
                ? ((robotFresh != null ? robotFresh : 0) * 100.0 / robotTotal) : 0.0;
        double coverageScore = (brandWebsiteRate + articleCoverImageRate + articleSourceUrlRate) / 3.0;

        overallScore = Math.round((robotScore * 0.4 + sourceScore * 0.2 + freshnessScore * 0.2 + coverageScore * 0.2) * 100.0) / 100.0;
    }
}