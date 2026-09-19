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
    public static final String CACHE_COMPANY_PREFIX = "robot:company:";
    public static final String CACHE_VIDEO_PREFIX = "robot:video:";
    public static final String CACHE_COMMUNITY_PREFIX = "robot:community:";

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

    // Phase9 缓存前缀
    public static final String CACHE_DISCOVERY_PREFIX = "robot:discovery:";
    public static final String CACHE_FEED_PREFIX = "robot:feed:";
    public static final String CACHE_GROWTH_PREFIX = "robot:growth:";
    public static final String CACHE_USER_PROFILE_PREFIX = "robot:user:profile:";
    public static final String CACHE_BRAND_DETAIL_PREFIX = "robot:brand:detail:";
    public static final String CACHE_COMPANY_DETAIL_PREFIX = "robot:company:detail:";
    public static final String CACHE_RECOMMEND_ROBOT_PREFIX = "robot:recommend:robot:";

    // Phase9 上榜原因代码
    public static final String REASON_HOT_RISE = "HOT_RISE";
    public static final String REASON_NEW_ENTRY = "NEW_ENTRY";
    public static final String REASON_SCORE_UP = "SCORE_UP";
    public static final String REASON_FAVORITE_BOOST = "FAVORITE_BOOST";
    public static final String REASON_DISCUSSION_ACTIVE = "DISCUSSION_ACTIVE";
    public static final String REASON_REVIEW_POSITIVE = "REVIEW_POSITIVE";
    public static final String REASON_FOLLOW_GROWTH = "FOLLOW_GROWTH";
    public static final String REASON_NEW_PRODUCT = "NEW_PRODUCT";

    // Phase9 推荐原因代码
    public static final String REC_SAME_CATEGORY = "SAME_CATEGORY";
    public static final String REC_SAME_BRAND = "SAME_BRAND";
    public static final String REC_TRENDING = "TRENDING";
    public static final String REC_FOLLOWED_ROBOT = "FOLLOWED_ROBOT";
    public static final String REC_PEOPLE_ALSO_VIEWED = "PEOPLE_ALSO_VIEWED";
    public static final String REC_POPULAR_ALTERNATIVE = "POPULAR_ALTERNATIVE";
    public static final String REC_RELATED_BRAND = "RELATED_BRAND";

    // Phase9 贡献分权重
    public static final int CONTRIB_REVIEW_PUBLISH = 10;
    public static final int CONTRIB_POST_PUBLISH = 5;
    public static final int CONTRIB_QUESTION_CREATE = 3;
    public static final int CONTRIB_ANSWER_CREATE = 5;
    public static final int CONTRIB_ANSWER_ACCEPTED = 10;
    public static final int CONTRIB_CORRECTION_ACCEPTED = 8;
    public static final int CONTRIB_HELPFUL_RECEIVED = 2;

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
    // Phase9 新增榜单类型
    public static final String RANK_FOLLOW = "follow";
    public static final String RANK_FAVORITE = "favorite";
    public static final String RANK_DISCUSSION = "discussion";
    public static final String RANK_REVIEW = "review";
    public static final String RANK_NEW_PRODUCT = "new_product";
    public static final String RANK_COMPANY_ATTENTION = "company_attention";

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
