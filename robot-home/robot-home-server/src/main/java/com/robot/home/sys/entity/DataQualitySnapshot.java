package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据质量快照Entity — Phase12 P0-6: BEFORE/AFTER持久化对比
 * 表名: data_quality_snapshot
 */
@Data
@TableName("data_quality_snapshot")
public class DataQualitySnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快照标签: BEFORE/AFTER/SNAPSHOT */
    private String label;
    /** 快照时间 */
    private LocalDateTime snapshotTime;
    /** 环境: PRODUCTION/STAGING/TEST */
    private String environment;

    // Robot Coverage
    private Long robotTotal;
    private Double robotCoverImageRate;
    private Double robotGalleryRate;
    private Double robotParamRate;
    private Double robotSourceUrlRate;
    private Double robotBrandRate;
    private Double robotCategoryRate;

    // Source Health
    private Long sourceTotal;
    private Long sourceHealthy;
    private Long sourceDegraded;
    private Long sourceFailed;
    private Long sourceUnknown;

    // Freshness
    private Long robotFresh;
    private Long robotAging;
    private Long robotStale;

    // Brand & Article
    private Long brandTotal;
    private Double brandWebsiteRate;
    private Long articleTotal;
    private Double articleCoverImageRate;
    private Double articleSourceUrlRate;

    // Crawler Pipeline
    private Long crawlerProductTotal;
    private Long crawlerProductPendingReview;
    private Long crawlerProductPublished;
    private Long crawlerArticleTotal;
    private Long crawlerArticlePendingReview;
    private Long crawlerArticlePublished;

    // Overall Score
    private Double overallScore;

    private LocalDateTime createTime;
}