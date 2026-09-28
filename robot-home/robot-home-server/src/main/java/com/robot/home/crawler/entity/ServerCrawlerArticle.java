package com.robot.home.crawler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Server端采集文章Entity（轻量级，仅用于统计查询）
 * Phase12: 数据质量统计
 */
@Data
@TableName("crawler_article")
public class ServerCrawlerArticle {
    private Long id;
    private String articleStatus;
    private String matchStatus;
    private Integer synced;
    private Long brandId;
    private Integer deleted;
}