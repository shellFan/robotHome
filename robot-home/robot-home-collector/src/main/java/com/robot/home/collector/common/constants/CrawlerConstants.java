package com.robot.home.collector.common.constants;

/**
 * 采集系统常量定义
 */
public class CrawlerConstants {

    private CrawlerConstants() {
    }

    // ==================== 数据源类型 ====================
    public static final String SOURCE_TYPE_WEBSITE = "WEBSITE";
    public static final String SOURCE_TYPE_RSS = "RSS";
    public static final String SOURCE_TYPE_SITEMAP = "SITEMAP";
    public static final String SOURCE_TYPE_WECHAT = "WECHAT";
    public static final String SOURCE_TYPE_MANUAL = "MANUAL";

    // ==================== 采集策略 ====================
    public static final String STRATEGY_GENERIC = "GENERIC";
    public static final String STRATEGY_SITEMAP = "SITEMAP";
    public static final String STRATEGY_RSS = "RSS";
    public static final String STRATEGY_NEWS = "NEWS";
    public static final String STRATEGY_PRODUCT = "PRODUCT";
    public static final String STRATEGY_WECHAT = "WECHAT";

    // ==================== 任务类型 ====================
    public static final String TASK_TYPE_FULL = "FULL";
    public static final String TASK_TYPE_INCREMENTAL = "INCREMENTAL";
    public static final String TASK_TYPE_SINGLE_URL = "SINGLE_URL";
    public static final String TASK_TYPE_RETRY = "RETRY";

    // ==================== 任务状态 ====================
    public static final String TASK_STATUS_QUEUED = "QUEUED";      // 已入队，等待被调度器CAS抢占
    public static final String TASK_STATUS_PENDING = "PENDING";    // (遗留兼容) 初始状态
    public static final String TASK_STATUS_RUNNING = "RUNNING";
    public static final String TASK_STATUS_SUCCESS = "SUCCESS";
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";
    public static final String TASK_STATUS_FAILED = "FAILED";
    public static final String TASK_STATUS_CANCELLED = "CANCELLED";
    public static final String TASK_STATUS_STOPPED = "STOPPED";

    // ==================== URL状态 ====================
    public static final String URL_STATUS_DISCOVERED = "DISCOVERED";
    public static final String URL_STATUS_QUEUED = "QUEUED";
    public static final String URL_STATUS_FETCHING = "FETCHING";
    public static final String URL_STATUS_SUCCESS = "SUCCESS";
    public static final String URL_STATUS_FAILED = "FAILED";
    public static final String URL_STATUS_SKIPPED = "SKIPPED";
    public static final String URL_STATUS_BLOCKED = "BLOCKED";

    // ==================== URL内容类型 ====================
    public static final String CONTENT_TYPE_HTML = "HTML";
    public static final String CONTENT_TYPE_RSS = "RSS";
    public static final String CONTENT_TYPE_IMAGE = "IMAGE";
    public static final String CONTENT_TYPE_JSON = "JSON";

    // ==================== 页面类型 ====================
    public static final String PAGE_TYPE_ARTICLE = "ARTICLE";
    public static final String PAGE_TYPE_PRODUCT = "PRODUCT";
    public static final String PAGE_TYPE_LIST = "LIST";
    public static final String PAGE_TYPE_OTHER = "OTHER";

    // ==================== 匹配状态 ====================
    public static final String MATCH_STATUS_PENDING = "PENDING";
    public static final String MATCH_STATUS_MATCHED = "MATCHED";
    public static final String MATCH_STATUS_UNMATCHED = "UNMATCHED";
    public static final String MATCH_STATUS_AUTO_MATCHED = "AUTO_MATCHED";
    public static final String MATCH_STATUS_MANUAL_MATCHED = "MANUAL_MATCHED";
    public static final String MATCH_STATUS_REJECTED = "REJECTED";

    // ==================== 匹配类型 ====================
    public static final String MATCH_TYPE_BRAND = "BRAND";
    public static final String MATCH_TYPE_COMPANY = "COMPANY";
    public static final String MATCH_TYPE_ROBOT = "ROBOT";

    // ==================== 业务类型 ====================
    public static final String BIZ_TYPE_ARTICLE = "ARTICLE";
    public static final String BIZ_TYPE_PRODUCT = "PRODUCT";

    // ==================== 文章状态 ====================
    public static final String ARTICLE_STATUS_CRAWLED = "CRAWLED";
    public static final String ARTICLE_STATUS_PARSED = "PARSED";
    public static final String ARTICLE_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String ARTICLE_STATUS_AUTO_APPROVED = "AUTO_APPROVED";
    public static final String ARTICLE_STATUS_PUBLISHED = "PUBLISHED";
    public static final String ARTICLE_STATUS_REJECTED = "REJECTED";
    public static final String ARTICLE_STATUS_DUPLICATE = "DUPLICATE";
    public static final String ARTICLE_STATUS_FAILED = "FAILED";

    // ==================== 产品状态 ====================
    public static final String PRODUCT_STATUS_CRAWLED = "CRAWLED";
    public static final String PRODUCT_STATUS_PARSED = "PARSED";
    public static final String PRODUCT_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String PRODUCT_STATUS_AUTO_APPROVED = "AUTO_APPROVED";
    public static final String PRODUCT_STATUS_PUBLISHED = "PUBLISHED";
    public static final String PRODUCT_STATUS_REJECTED = "REJECTED";
    public static final String PRODUCT_STATUS_DUPLICATE = "DUPLICATE";
    public static final String PRODUCT_STATUS_FAILED = "FAILED";

    // ==================== 媒体类型 ====================
    public static final String MEDIA_TYPE_IMAGE = "IMAGE";
    public static final String MEDIA_TYPE_VIDEO = "VIDEO";

    // ==================== 下载状态 ====================
    public static final String DOWNLOAD_STATUS_PENDING = "PENDING";
    public static final String DOWNLOAD_STATUS_SUCCESS = "SUCCESS";
    public static final String DOWNLOAD_STATUS_FAILED = "FAILED";

    // ==================== 错误类型 ====================
    public static final String ERROR_TYPE_NETWORK = "NETWORK";
    public static final String ERROR_TYPE_TIMEOUT = "TIMEOUT";
    public static final String ERROR_TYPE_HTTP_403 = "HTTP_403";
    public static final String ERROR_TYPE_HTTP_404 = "HTTP_404";
    public static final String ERROR_TYPE_PARSE_ERROR = "PARSE_ERROR";
    public static final String ERROR_TYPE_JS_RENDER_ERROR = "JS_RENDER_ERROR";
    public static final String ERROR_TYPE_DUPLICATE = "DUPLICATE";
    public static final String ERROR_TYPE_DATABASE = "DATABASE";
    public static final String ERROR_TYPE_IMAGE = "IMAGE";
    public static final String ERROR_TYPE_BLOCKED = "BLOCKED";
    public static final String ERROR_TYPE_UNKNOWN = "UNKNOWN";

    // ==================== 规则类型 ====================
    public static final String RULE_TYPE_SELECTOR = "SELECTOR";
    public static final String RULE_TYPE_REGEX = "REGEX";
    public static final String RULE_TYPE_KEYWORD = "KEYWORD";
    public static final String RULE_TYPE_PARAM_MAP = "PARAM_MAP";
    public static final String RULE_TYPE_CATEGORY = "CATEGORY";

    // ==================== 日志级别 ====================
    public static final String LOG_LEVEL_INFO = "INFO";
    public static final String LOG_LEVEL_WARN = "WARN";
    public static final String LOG_LEVEL_ERROR = "ERROR";

    // ==================== 日志结果 ====================
    public static final String LOG_RESULT_SUCCESS = "SUCCESS";
    public static final String LOG_RESULT_FAILED = "FAILED";
    public static final String LOG_RESULT_SKIPPED = "SKIPPED";

    // ==================== 品牌别名类型 ====================
    public static final String ALIAS_TYPE_NAME = "NAME";
    public static final String ALIAS_TYPE_ENGLISH = "ENGLISH";
    public static final String ALIAS_TYPE_ABBREVIATION = "ABBREVIATION";
    public static final String ALIAS_TYPE_OTHER = "OTHER";

    // ==================== 通用状态 ====================
    public static final Integer STATUS_DISABLED = 0;
    public static final Integer STATUS_ENABLED = 1;

    // ==================== 同步状态 ====================
    public static final Integer SYNCED_NO = 0;
    public static final Integer SYNCED_YES = 1;

    // ==================== 解决状态 ====================
    public static final Integer RESOLVED_NO = 0;
    public static final Integer RESOLVED_YES = 1;
}