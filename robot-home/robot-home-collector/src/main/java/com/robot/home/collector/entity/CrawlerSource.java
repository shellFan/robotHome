package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 采集数据源配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_source")
public class CrawlerSource extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源名称 */
    private String sourceName;

    /** 数据源类型: WEBSITE/RSS/SITEMAP/WECHAT/MANUAL */
    private String sourceType;

    /** 基础URL */
    private String baseUrl;

    /** 关联品牌ID */
    private Long brandId;

    /** 关联企业ID */
    private Long companyId;

    /** 是否启用采集: 0停用 1启用 */
    private Integer crawlEnabled;

    /** 采集策略: GENERIC/SITEMAP/RSS/NEWS/PRODUCT/WECHAT */
    private String crawlStrategy;

    /** 采集间隔(秒) */
    private Integer crawlInterval;

    /** 最大爬取深度 */
    private Integer maxDepth;

    /** 单次最大页面数 */
    private Integer maxPages;

    /** 包含URL规则(正则,逗号分隔) */
    private String includeRules;

    /** 排除URL规则(正则,逗号分隔) */
    private String excludeRules;

    /** 标题CSS选择器 */
    private String titleSelector;

    /** 正文CSS选择器 */
    private String contentSelector;

    /** 日期CSS选择器 */
    private String dateSelector;

    /** 作者CSS选择器 */
    private String authorSelector;

    /** 封面CSS选择器 */
    private String coverSelector;

    /** 列表页CSS选择器 */
    private String listSelector;

    /** 详情链接CSS选择器 */
    private String detailLinkSelector;

    /** 是否自动发布: 0需审核 1自动发布 */
    private Integer autoPublish;

    /** 是否遵守robots.txt */
    private Integer respectRobots;

    /** 请求间隔(毫秒) */
    private Integer requestDelay;

    /** 自定义User-Agent */
    private String userAgent;

    /** 额外配置JSON */
    private String extraConfig;

    /** 最后采集时间 */
    private LocalDateTime lastCrawlTime;

    /** 最后采集状态 */
    private String lastCrawlStatus;

    /** 累计文章数 */
    private Integer totalArticles;

    /** 累计产品数 */
    private Integer totalProducts;

    /** 累计错误数 */
    private Integer totalErrors;

    /** 状态: 0禁用 1正常 */
    private Integer status;

    /** 种子URL列表(换行分隔) */
    private String seedUrls;

    /** Sitemap URL */
    private String sitemapUrl;

    /** 抓取模式: http/browser */
    private String fetchMode;

    /** 是否跟踪外部链接 */
    private Integer followExternal;

    /** 等待选择器(browser模式) */
    private String waitSelector;

    /** 页面加载后等待时间(browser模式,毫秒) */
    private Integer waitAfterLoadMs;
}