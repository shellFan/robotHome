package com.robot.home.crawler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Server端采集产品Entity（轻量级，仅用于统计查询）
 * Phase12: 数据质量统计
 */
@Data
@TableName("crawler_product")
public class ServerCrawlerProduct {
    private Long id;
    private String productStatus;
    private String matchStatus;
    private Integer synced;
    private Long brandId;
    private Long robotId;
    private Integer deleted;
}