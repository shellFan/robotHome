package com.robot.home.crawler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Server端采集源Entity（轻量级，仅用于统计查询）
 * Phase12: 数据质量统计 + Source健康度
 */
@Data
@TableName("crawler_source")
public class ServerCrawlerSource {
    private Long id;
    private String sourceName;
    private String sourceType;
    private String healthStatus;
    private String trustLevel;
    private Integer crawlEnabled;
    private Integer status;
    private Long brandId;
    private Integer consecutiveFailures;
    private Integer avgLatencyMs;
    private Integer totalCrawls;
    private Integer totalSuccessCrawls;
    private Integer deleted;
}