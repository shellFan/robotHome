package com.robot.home.seo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * SEO页面访问事件
 * 用于增长漏斗分析, ip_hash替代原始IP(隐私保护)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seo_page_view")
public class SeoPageView extends BaseEntity {

    /** 页面类型: robot_detail/brand_detail/company_detail/article_detail/home/category */
    private String pageType;

    /** 页面slug(如 robot/xxx) */
    private String pageSlug;

    /** 页面完整路径 */
    private String pagePath;

    /** 关联实体ID(如robotId/brandId) */
    private Long entityId;

    /** 登录用户ID；匿名访客为空 */
    private Long userId;

    /** 访客ID(匿名指纹或登录用户ID) */
    private String visitorId;

    /** IP哈希(SHA-256前64位, 不存原始IP) */
    private String ipHash;

    /** User-Agent */
    private String userAgent;

    /** 来源: direct/search/social/referral/internal */
    private String refererSource;

    /** 来源URL */
    private String refererUrl;

    /** 访问时间 */
    private LocalDateTime viewTime;
}
