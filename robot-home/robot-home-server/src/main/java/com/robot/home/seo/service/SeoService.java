package com.robot.home.seo.service;

import java.util.Map;

/**
 * SEO服务
 * <p>
 * 职责:
 * 1. sitemap.xml 生成(robots/brands/companies/articles)
 * 2. robots.txt 生成
 * 3. 页面访问事件记录(增长漏斗数据源)
 * 4. slug生成与回填
 */
public interface SeoService {

    /**
     * 生成 sitemap.xml 内容
     *
     * @param baseUrl 站点基础URL, 如 https://robot-home.example.com
     * @return XML字符串
     */
    String generateSitemap(String baseUrl);

    /**
     * 生成 robots.txt 内容
     *
     * @param baseUrl 站点基础URL
     * @return robots.txt文本
     */
    String generateRobotsTxt(String baseUrl);

    /**
     * 记录页面访问事件
     *
     * @param pageType      页面类型: robot_detail/brand_detail/company_detail/article_detail/home/category
     * @param pageSlug      页面slug
     * @param pagePath      页面路径
     * @param entityId      关联实体ID
     * @param visitorId     访客ID(匿名指纹或登录用户ID)
     * @param ipHash        IP哈希
     * @param userAgent     User-Agent
     * @param refererSource 来源类型
     * @param refererUrl    来源URL
     */
    void recordPageView(String pageType, String pageSlug, String pagePath, Long entityId, Long userId,
                        String visitorId, String ipHash, String userAgent,
                        String refererSource, String refererUrl);

    /**
     * 为缺少slug的实体批量生成slug
     *
     * @return 统计信息: {"robotUpdated":10, "brandUpdated":5, ...}
     */
    Map<String, Integer> backfillSlugs();

    /**
     * 页面访问统计(最近N天)
     *
     * @param days 天数
     * @return 统计Map: {"totalViews":1000, "uniqueVisitors":300, "byPageType":{...}}
     */
    Map<String, Object> pageViewStats(int days);
}
