package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * URL队列
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_url")
public class CrawlerUrl extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private Long sourceId;

    /** 任务ID */
    private Long taskId;

    /** 原始URL */
    private String url;

    /** 规范化URL */
    private String normalizedUrl;

    /** URL SHA-256哈希 */
    private String urlHash;

    /** 父页面URL */
    private String parentUrl;

    /** 爬取深度 */
    private Integer depth;

    /** 内容类型: HTML/RSS/IMAGE/JSON */
    private String contentType;

    /** URL状态: DISCOVERED/QUEUED/FETCHING/SUCCESS/FAILED/SKIPPED/BLOCKED */
    private String urlStatus;

    /** HTTP状态码 */
    private Integer httpStatus;

    /** 重试次数 */
    private Integer retryCount;

    /** 最后错误 */
    private String lastError;

    /** HTTP ETag */
    private String etag;

    /** HTTP Last-Modified */
    private String lastModified;

    /** 抓取时间 */
    private LocalDateTime crawlTime;
}