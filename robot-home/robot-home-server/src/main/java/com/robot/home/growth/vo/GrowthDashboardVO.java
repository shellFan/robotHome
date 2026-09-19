package com.robot.home.growth.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Phase9: 增长Dashboard视图
 */
@Data
public class GrowthDashboardVO {

    /** 今日概览 */
    private TodayStats today;

    /** 7天趋势 */
    private List<DailyStatItem> trend7d;

    /** 30天趋势 */
    private List<DailyStatItem> trend30d;

    /** 转化漏斗 */
    private FunnelItem funnel;

    /** Top热门机器人 */
    private List<TopItem> topRobots;

    /** Top热门品牌 */
    private List<TopItem> topBrands;

    /** Top搜索关键词 */
    private List<TopItem> topSearchKeywords;

    @Data
    public static class TodayStats {
        private Integer dau;
        private Integer newUsers;
        private Integer robotViews;
        private Integer searches;
        private Integer favorites;
        private Integer compares;
        private Integer questions;
        private Integer answers;
        private Integer posts;
        private Integer reviews;
        private Integer inquiries;
    }

    @Data
    public static class DailyStatItem {
        private String date;
        private Integer dau;
        private Integer newUsers;
        private Integer robotViews;
        private Integer searches;
        private Integer favorites;
        private Integer questions;
        private Integer posts;
    }

    @Data
    public static class FunnelItem {
        /** 浏览 */
        private Integer views;
        /** 对比/收藏 */
        private Integer engages;
        /** 选型 */
        private Integer selections;
        /** 询价/采购 */
        private Integer inquiries;
    }

    @Data
    public static class TopItem {
        private Long id;
        private String name;
        private Integer count;
    }
}