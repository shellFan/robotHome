package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采集媒体(图片)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_media")
public class CrawlerMedia extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private Long sourceId;

    /** 原始URL */
    private String originalUrl;

    /** URL哈希 */
    private String urlHash;

    /** 本地存储URL */
    private String storageUrl;

    /** 文件内容哈希 */
    private String fileHash;

    /** 文件大小 */
    private Long fileSize;

    /** MIME类型 */
    private String mimeType;

    /** 宽度 */
    private Integer width;

    /** 高度 */
    private Integer height;

    /** 媒体类型: IMAGE/VIDEO */
    private String mediaType;

    /** 下载状态: PENDING/SUCCESS/FAILED */
    private String downloadStatus;

    /** 重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMessage;

    /** 关联文章ID */
    private Long refArticleId;

    /** 关联产品ID */
    private Long refProductId;
}