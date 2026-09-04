package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 采集文章
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_article")
public class CrawlerArticle extends BaseEntity {

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

    /** 标题 */
    private String title;

    /** 副标题 */
    private String subtitle;

    /** 作者 */
    private String author;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 封面图(原始URL) */
    private String coverImage;

    /** 封面图(本地URL) */
    private String coverImageLocal;

    /** 摘要 */
    private String summary;

    /** 正文HTML */
    private String contentHtml;

    /** 正文纯文本 */
    private String contentText;

    /** 图片列表JSON */
    private String images;

    /** 标签JSON */
    private String tags;

    /** 分类 */
    private String category;

    /** 内容哈希 */
    private String contentHash;

    /** SimHash */
    private String simhash;

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

    /** 文章状态: CRAWLED/PARSED/PENDING_REVIEW/AUTO_APPROVED/PUBLISHED/REJECTED/DUPLICATE/FAILED */
    private String articleStatus;

    /** 同步到article表后的ID */
    private Long articleId;

    /** 是否已同步: 0未同步 1已同步 */
    private Integer synced;

    /** 抓取时间 */
    private LocalDateTime crawlTime;
}