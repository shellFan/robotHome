package com.robot.home.sys.vo;

import lombok.Data;

import java.util.Map;

/**
 * 数据质量统计VO
 * Phase12: 真实数据成绩报告 - 图片/参数/来源覆盖率
 */
@Data
public class DataQualityVO {

    // ===== 机器人数据质量 =====
    /** 机器人总数 */
    private Long robotTotal;
    /** 有封面图的机器人 */
    private Long robotWithCoverImage;
    /** 封面图覆盖率(%) */
    private Double robotCoverImageRate;
    /** 有图集的机器人(images非空) */
    private Long robotWithGallery;
    /** 图集覆盖率(%) */
    private Double robotGalleryRate;
    /** 有参数的机器人(mainParams非空) */
    private Long robotWithParams;
    /** 参数覆盖率(%) */
    private Double robotParamRate;
    /** 有来源URL的机器人 */
    private Long robotWithSourceUrl;
    /** 来源URL覆盖率(%) */
    private Double robotSourceUrlRate;
    /** 有品牌的机器人 */
    private Long robotWithBrand;
    /** 品牌覆盖率(%) */
    private Double robotBrandRate;
    /** 有分类的机器人 */
    private Long robotWithCategory;
    /** 分类覆盖率(%) */
    private Double robotCategoryRate;

    // ===== 按数据来源分组 =====
    /** 按dataSource分组统计 */
    private Map<String, Long> robotByDataSource;

    // ===== 品牌数据质量 =====
    /** 品牌总数 */
    private Long brandTotal;
    /** 有官网的品牌 */
    private Long brandWithWebsite;
    /** 官网覆盖率(%) */
    private Double brandWebsiteRate;

    // ===== 文章数据质量 =====
    /** 文章总数 */
    private Long articleTotal;
    /** 有封面图的文章 */
    private Long articleWithCoverImage;
    /** 封面图覆盖率(%) */
    private Double articleCoverImageRate;
    /** 有来源URL的文章 */
    private Long articleWithSourceUrl;
    /** 来源URL覆盖率(%) */
    private Double articleSourceUrlRate;

    // ===== 采集器数据概览 =====
    /** 采集产品总数 */
    private Long crawlerProductTotal;
    /** 采集文章总数 */
    private Long crawlerArticleTotal;
    /** 待审核产品 */
    private Long crawlerProductPendingReview;
    /** 待审核文章 */
    private Long crawlerArticlePendingReview;
    /** 已发布产品 */
    private Long crawlerProductPublished;
    /** 已发布文章 */
    private Long crawlerArticlePublished;
    /** 重复被拒产品 */
    private Long crawlerProductDuplicate;
    /** 重复被拒文章 */
    private Long crawlerArticleDuplicate;

    // ===== Phase12: Source健康度统计 =====
    /** 采集源总数 */
    private Long sourceTotal;
    /** HEALTHY源数量 */
    private Long sourceHealthy;
    /** DEGRADED源数量 */
    private Long sourceDegraded;
    /** FAILED源数量 */
    private Long sourceFailed;
    /** DISABLED源数量 */
    private Long sourceDisabled;
    /** UNKNOWN源数量(从未运行) */
    private Long sourceUnknown;
    /** OFFICIAL源数量 */
    private Long sourceOfficial;
    /** TRUSTED源数量 */
    private Long sourceTrusted;
    /** NORMAL源数量 */
    private Long sourceNormal;

    // ===== Phase12: Robot Freshness统计 =====
    /** FRESH机器人数量(≤30天) */
    private Long robotFresh;
    /** AGING机器人数量(31-90天) */
    private Long robotAging;
    /** STALE机器人数量(>90天) */
    private Long robotStale;
}