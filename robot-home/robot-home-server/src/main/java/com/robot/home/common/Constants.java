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
}
