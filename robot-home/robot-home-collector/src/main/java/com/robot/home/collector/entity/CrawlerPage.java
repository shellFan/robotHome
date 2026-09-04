package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 页面原始内容
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_page")
public class CrawlerPage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** URL ID */
    private Long urlId;

    /** 数据源ID */
    private Long sourceId;

    /** 任务ID */
    private Long taskId;

    /** 页面URL */
    private String url;

    /** 页面标题 */
    private String title;

    /** 原始HTML */
    private String htmlContent;

    /** 提取的文本 */
    private String textContent;

    /** 内容SHA-256哈希 */
    private String contentHash;

    /** SimHash指纹 */
    private String simhash;

    /** 页面类型: ARTICLE/PRODUCT/LIST/OTHER */
    private String pageType;

    /** 页面链接数 */
    private Integer linksCount;

    /** 页面图片数 */
    private Integer imagesCount;

    /** 抓取时间 */
    private LocalDateTime crawlTime;
}