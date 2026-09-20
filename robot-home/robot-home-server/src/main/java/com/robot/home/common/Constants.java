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

    // Phase10 缓存前缀
    public static final String CACHE_TRUST_PREFIX = "robot:trust:";
    public static final String CACHE_SUBSCRIPTION_PREFIX = "robot:sub:";
    public static final String CACHE_NOTIFICATION_UNREAD_PREFIX = "robot:notify:unread:";
    public static final String CACHE_REPUTATION_PREFIX = "robot:reputation:";
    public static final String CACHE_COLLECTION_PREFIX = "robot:collection:";

    // Phase10 Trust等级
    public static final String TRUST_VERIFIED = "VERIFIED";
    public static final String TRUST_HIGH = "HIGH";
    public static final String TRUST_NORMAL = "NORMAL";
    public static final String TRUST_LOW = "LOW";

    // Phase10 Trust来源类型
    public static final String SOURCE_OFFICIAL = "OFFICIAL";
    public static final String SOURCE_VENDOR_SITE = "VENDOR_SITE";
    public static final String SOURCE_TRUSTED_MEDIA = "TRUSTED_MEDIA";
    public static final String SOURCE_USER_CORRECTION = "USER_CORRECTION";
    public static final String SOURCE_CRAWLER = "CRAWLER";
    public static final String SOURCE_MANUAL = "MANUAL";

    // Phase10 变更类型
    public static final String CHANGE_PRICE = "PRICE";
    public static final String CHANGE_PARAMETER = "PARAMETER";
    public static final String CHANGE_VERSION = "VERSION";
    public static final String CHANGE_STATUS = "STATUS";
    public static final String CHANGE_BASIC_INFO = "BASIC_INFO";

    // Phase10 订阅事件类型
    public static final String SUB_EVENT_NEW_CONTENT = "NEW_CONTENT";
    public static final String SUB_EVENT_PARAM_CHANGE = "PARAM_CHANGE";
    public static final String SUB_EVENT_PRICE_CHANGE = "PRICE_CHANGE";
    public static final String SUB_EVENT_NEW_REVIEW = "NEW_REVIEW";
    public static final String SUB_EVENT_NEW_QA = "NEW_QA";
    public static final String SUB_EVENT_PRODUCT_RELEASE = "PRODUCT_RELEASE";
    public static final String SUB_EVENT_IMPORTANT_CHANGE = "IMPORTANT_CHANGE";

    // Phase10 订阅目标类型
    public static final String SUB_TARGET_ROBOT = "ROBOT";
    public static final String SUB_TARGET_BRAND = "BRAND";
    public static final String SUB_TARGET_COMPANY = "COMPANY";

    // Phase10 通知类型
    public static final String NOTIFY_SYSTEM = "SYSTEM";
    public static final String NOTIFY_FOLLOW_UPDATE = "FOLLOW_UPDATE";
    public static final String NOTIFY_PARAM_CHANGE = "PARAM_CHANGE";
    public static final String NOTIFY_PRICE_CHANGE = "PRICE_CHANGE";
    public static final String NOTIFY_QUESTION_ANSWER = "QUESTION_ANSWER";
    public static final String NOTIFY_ANSWER_ACCEPTED = "ANSWER_ACCEPTED";
    public static final String NOTIFY_REVIEW_INTERACTION = "REVIEW_INTERACTION";
    public static final String NOTIFY_POST_INTERACTION = "POST_INTERACTION";
    public static final String NOTIFY_PROCUREMENT_RESPONSE = "PROCUREMENT_RESPONSE";

    // Phase10 Pipeline状态
    public static final String PIPELINE_NEW = "NEW";
    public static final String PIPELINE_CONTACTED = "CONTACTED";
    public static final String PIPELINE_QUALIFIED = "QUALIFIED";
    public static final String PIPELINE_MATCHING = "MATCHING";
    public static final String PIPELINE_RESPONDED = "RESPONDED";
    public static final String PIPELINE_NEGOTIATING = "NEGOTIATING";
    public static final String PIPELINE_WON = "WON";
    public static final String PIPELINE_LOST = "LOST";
    public static final String PIPELINE_CLOSED = "CLOSED";

    // Phase10 响应状态
    public static final String RESPONSE_SUBMITTED = "SUBMITTED";
    public static final String RESPONSE_VIEWED = "VIEWED";
    public static final String RESPONSE_CONTACTED = "CONTACTED";
    public static final String RESPONSE_ACCEPTED = "ACCEPTED";
    public static final String RESPONSE_REJECTED = "REJECTED";
    public static final String RESPONSE_WITHDRAWN = "WITHDRAWN";

    // Phase10 信誉等级
    public static final String REP_NEW = "NEW";
    public static final String REP_CONTRIBUTOR = "CONTRIBUTOR";
    public static final String REP_ACTIVE = "ACTIVE";
    public static final String REP_TRUSTED = "TRUSTED";
    public static final String REP_EXPERT = "EXPERT";

    // Phase10 信誉事件类型
    public static final String REP_EVENT_CORRECTION_ACCEPTED = "CORRECTION_ACCEPTED";
    public static final String REP_EVENT_ANSWER_ACCEPTED = "ANSWER_ACCEPTED";
    public static final String REP_EVENT_REVIEW_HELPFUL = "REVIEW_HELPFUL";
    public static final String REP_EVENT_POST_QUALITY = "POST_QUALITY";
    public static final String REP_EVENT_QUESTION_ANSWERED = "QUESTION_ANSWERED";
    public static final String REP_EVENT_ABUSE_PENALTY = "ABUSE_PENALTY";
    public static final String REP_EVENT_SPAM_REJECTED = "SPAM_REJECTED";

    // Phase10 信誉分权重
    public static final int REP_SCORE_CORRECTION_ACCEPTED = 15;
    public static final int REP_SCORE_ANSWER_ACCEPTED = 12;
    public static final int REP_SCORE_REVIEW_HELPFUL = 5;
    public static final int REP_SCORE_POST_QUALITY = 8;
    public static final int REP_SCORE_QUESTION_ANSWERED = 3;
    public static final int REP_SCORE_ABUSE_PENALTY = -20;
    public static final int REP_SCORE_SPAM_REJECTED = -10;

    // Phase10 信誉等级阈值
    public static final int REP_THRESHOLD_CONTRIBUTOR = 30;
    public static final int REP_THRESHOLD_ACTIVE = 100;
    public static final int REP_THRESHOLD_TRUSTED = 300;
    public static final int REP_THRESHOLD_EXPERT = 800;

    // Phase10 CRM跟进操作
    public static final String CRM_ACTION_STATUS_CHANGE = "STATUS_CHANGE";
    public static final String CRM_ACTION_FOLLOW_UP = "FOLLOW_UP";
    public static final String CRM_ACTION_CALL = "CALL";
    public static final String CRM_ACTION_EMAIL = "EMAIL";
    public static final String CRM_ACTION_MEETING = "MEETING";
    public static final String CRM_ACTION_NOTE = "NOTE";
    public static final String CRM_ACTION_ASSIGN = "ASSIGN";

    // Phase10 可见性
    public static final String VIS_PRIVATE = "PRIVATE";
    public static final String VIS_PUBLIC = "PUBLIC";

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
