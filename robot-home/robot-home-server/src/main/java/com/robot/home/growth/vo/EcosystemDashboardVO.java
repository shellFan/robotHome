package com.robot.home.growth.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * P0-8: 生态Dashboard视图
 * 覆盖Phase10新增指标: 回访率、订阅、通知、贡献、信誉、采购CRM、转化
 */
@Data
public class EcosystemDashboardVO {

    /** 今日生态概览 */
    private EcosystemToday today;

    /** 7天生态趋势 */
    private List<EcosystemDailyItem> trend7d;

    /** 30天生态趋势 */
    private List<EcosystemDailyItem> trend30d;

    /** 通知CTR趋势 */
    private List<NotificationCtrItem> notificationCtrTrend;

    /** 信誉等级分布 */
    private List<LevelDistributionItem> reputationDistribution;

    /** Trust等级分布 */
    private List<LevelDistributionItem> trustDistribution;

    /** Pipeline转化漏斗 */
    private PipelineFunnel pipelineFunnel;

    /** 订阅类型分布 */
    private List<SubscriptionTypeItem> subscriptionTypeDistribution;

    /** 今日生态概览 */
    @Data
    public static class EcosystemToday {
        /** 回访用户数 */
        private Integer returningUsers;
        /** 新增订阅数 */
        private Integer subscriptionCount;
        /** 通知发送数 */
        private Integer notificationSent;
        /** 通知点击数 */
        private Integer notificationClicked;
        /** 通知CTR(%) */
        private Double notificationCtr;
        /** 贡献用户数 */
        private Integer contributionUsers;
        /** 被采纳纠错数 */
        private Integer acceptedCorrections;
        /** 采购线索数 */
        private Integer procurementLeads;
        /** 企业响应数 */
        private Integer enterpriseResponses;
        /** 跟进转化数 */
        private Integer followConversions;
    }

    /** 生态日趋势项 */
    @Data
    public static class EcosystemDailyItem {
        private String date;
        private Integer returningUsers;
        private Integer subscriptionCount;
        private Integer notificationSent;
        private Integer notificationClicked;
        private Integer contributionUsers;
        private Integer procurementLeads;
        private Integer enterpriseResponses;
    }

    /** 通知CTR趋势项 */
    @Data
    public static class NotificationCtrItem {
        private String date;
        /** CTR百分比 */
        private Double ctr;
        private Integer sent;
        private Integer clicked;
    }

    /** 等级分布项 */
    @Data
    public static class LevelDistributionItem {
        private String level;
        private String label;
        private Integer count;
    }

    /** Pipeline转化漏斗 */
    @Data
    public static class PipelineFunnel {
        private Integer newLeads;
        private Integer contacted;
        private Integer qualified;
        private Integer matching;
        private Integer responded;
        private Integer negotiating;
        private Integer won;
        private Integer lost;
        private Integer closed;
    }

    /** 订阅类型分布 */
    @Data
    public static class SubscriptionTypeItem {
        private String targetType;
        private Integer count;
    }
}