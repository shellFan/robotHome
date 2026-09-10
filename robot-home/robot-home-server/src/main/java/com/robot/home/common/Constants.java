package com.robot.home.common;

/**
 * 全局常量
 */
public final class Constants {

    private Constants() {
    }

    // 逻辑删除
    public static final int DELETED_NO = 0;
    public static final int DELETED_YES = 1;

    // 通用状态
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    // 缓存前缀
    public static final String CACHE_TOKEN_PREFIX = "robot:token:";
    public static final String CACHE_SMS_CODE_PREFIX = "robot:sms:code:";
    public static final String CACHE_CAPTCHA_PREFIX = "robot:captcha:";
    public static final String CACHE_HOT_PREFIX = "robot:hot:";
    public static final String CACHE_RANKING_PREFIX = "robot:ranking:";
    public static final String CACHE_SEARCH_HOT_PREFIX = "robot:search:hot:";
    public static final String CACHE_LIMIT_PREFIX = "robot:limit:";
    public static final String CACHE_BANNER_PREFIX = "robot:banner:";
    public static final String CACHE_RECOMMEND_PREFIX = "robot:recommend:";
    public static final String CACHE_BRAND_PREFIX = "robot:brand:";
    public static final String CACHE_CATEGORY_PREFIX = "robot:category:";
    public static final String CACHE_FILTER_PREFIX = "robot:filter:";
    public static final String CACHE_ARTICLE_CATEGORY_PREFIX = "robot:article:cat:";
    public static final String CACHE_ARTICLE_HOT_PREFIX = "robot:article:hot:";

    // 行为事件
    public static final String CACHE_BEHAVIOR_DEDUP_PREFIX = "robot:hot:dedup:";  // 去重前缀
    public static final long BEHAVIOR_DEDUP_WINDOW_SECONDS = 60L;  // 去重窗口60秒
    public static final String CACHE_HOT_ZSET_PREFIX = "robot:hot:";  // 热度ZSET前缀
    public static final long BEHAVIOR_ZSET_EXPIRE_DAYS = 7L;  // ZSET默认过期天数

    // 搜索建议
    public static final String CACHE_SEARCH_SUGGESTION_PREFIX = "robot:search:suggest:";

    // 排行榜配置缓存
    public static final String CACHE_RANKING_WEIGHT_PREFIX = "robot:ranking:weight:";
    public static final String CACHE_RANKING_DECAY_PREFIX = "robot:ranking:decay:";

    // 登录 token 有效期（与 jwt.expiration 保持一致，单位秒）
    public static final long TOKEN_EXPIRE_SECONDS = 86400L;

    // 默认角色
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    // 用户类型
    public static final int USER_TYPE_NORMAL = 1;   // 普通用户
    public static final int USER_TYPE_ADMIN = 2;    // 后台管理员

    // 登录方式
    public static final String LOGIN_TYPE_PASSWORD = "password";
    public static final String LOGIN_TYPE_SMS = "sms";
    public static final String LOGIN_TYPE_WX = "wechat";

    // 业务类型（收藏/点赞/关注/浏览通用）
    public static final String BIZ_TYPE_ROBOT = "robot";
    public static final String BIZ_TYPE_ARTICLE = "article";
    public static final String BIZ_TYPE_VIDEO = "video";
    public static final String BIZ_TYPE_POST = "post";
    public static final String BIZ_TYPE_TUTORIAL = "tutorial";
    public static final String BIZ_TYPE_BRAND = "brand";
    public static final String BIZ_TYPE_COMPANY = "company";
    public static final String BIZ_TYPE_USER = "user";

    // 排行榜类型
    public static final String RANK_HOT = "hot";
    public static final String RANK_HUMANOID = "humanoid";
    public static final String RANK_QUADRUPED = "quadruped";
    public static final String RANK_SERVICE = "service";
    public static final String RANK_INDUSTRIAL = "industrial";
    public static final String RANK_FAMILY = "family";
    public static final String RANK_DEV = "dev";

    // 询价状态
    public static final int INQUIRY_PENDING = 1;     // 待处理
    public static final int INQUIRY_PROCESSING = 2;  // 处理中
    public static final int INQUIRY_CONTACTED = 3;   // 已联系
    public static final int INQUIRY_DEAL = 4;        // 已成交
    public static final int INQUIRY_CLOSED = 5;      // 已关闭

    // 社区帖子类型
    public static final String POST_TYPE_DISCUSSION = "discussion";
    public static final String POST_TYPE_QUESTION = "question";
    public static final String POST_TYPE_DIY = "diy";
    public static final String POST_TYPE_REVIEW = "review";
    public static final String POST_TYPE_NEWS_SHARE = "news_share";

    // 用户反馈类型
    public static final String FEEDBACK_TYPE_BUG = "bug";
    public static final String FEEDBACK_TYPE_FEATURE = "feature";
    public static final String FEEDBACK_TYPE_IMPROVEMENT = "improvement";
    public static final String FEEDBACK_TYPE_OTHER = "other";

    // 对比参数类型
    public static final String COMPARE_HIGHER_BETTER = "HIGHER_BETTER";
    public static final String COMPARE_LOWER_BETTER = "LOWER_BETTER";
    public static final String COMPARE_NEUTRAL = "NEUTRAL";
    public static final String COMPARE_BOOLEAN = "BOOLEAN";
    public static final String COMPARE_TEXT = "TEXT";
}
