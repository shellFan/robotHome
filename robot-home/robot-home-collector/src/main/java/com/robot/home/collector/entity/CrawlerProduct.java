package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采集产品
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_product")
public class CrawlerProduct extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private Long sourceId;

    /** 任务ID */
    private Long taskId;

    /** 页面ID */
    private Long pageId;

    /** 来源名称 */
    private String sourceName;

    /** 来源URL */
    private String sourceUrl;

    /** 来源站点 */
    private String sourceSite;

    /** 产品名称 */
    private String productName;

    /** 型号 */
    private String model;

    /** 品牌名称(原始) */
    private String brandName;

    /** 分类 */
    private String category;

    /** 摘要 */
    private String summary;

    /** 描述HTML */
    private String description;

    /** 封面图(原始URL) */
    private String coverImage;

    /** 封面图(本地URL) */
    private String coverImageLocal;

    /** 图集JSON(原始URL) */
    private String gallery;

    /** 图集JSON(本地URL) */
    private String galleryLocal;

    /** 产品状态: 在售/预售/停产 */
    private String status;

    /** 发布日期 */
    private LocalDate releaseDate;

    /** 价格(原始) */
    private String price;

    /** 官方链接 */
    private String officialUrl;

    /** 原始参数JSON */
    private String rawParams;

    /** 标准化参数JSON */
    private String normalizedParams;

    /** 内容哈希 */
    private String contentHash;

    /** 匹配品牌ID */
    private Long brandId;

    /** 匹配分类ID */
    private Long categoryId;

    /** 匹配系列ID */
    private Long seriesId;

    /** 匹配企业ID */
    private Long companyId;

    /** 匹配机器人ID */
    private Long robotId;

    /** 匹配状态: PENDING/MATCHED/UNMATCHED */
    private String matchStatus;

    /** 产品状态: CRAWLED/PARSED/PENDING_REVIEW/AUTO_APPROVED/PUBLISHED/REJECTED/DUPLICATE/FAILED */
    private String productStatus;

    /** 同步到robot表后的ID */
    private Long robotIdSynced;

    /** 是否已同步: 0未同步 1已同步 */
    private Integer synced;

    /** 失败原因 */
    private String failReason;

    /** 重试次数 */
    private Integer retryCount;

    /** 下次重试时间（指数退避） */
    private LocalDateTime nextRetryTime;

    /** 抓取时间 */
    private LocalDateTime crawlTime;
}