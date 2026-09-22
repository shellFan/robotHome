package com.robot.home.growth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * Phase9: 增长日统计
 * 对应表: growth_daily_stat
 */
@Data
@TableName("growth_daily_stat")
public class GrowthDailyStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计日期 */
    private LocalDate statDate;

    /** DAU（日活跃用户）- DB列名active_users */
    @TableField("active_users")
    private Integer dau;

    /** 新注册用户数 */
    private Integer newUsers;

    /** 机器人浏览数 */
    private Integer robotViews;

    /** 搜索次数 */
    private Integer searches;

    /** 收藏次数 */
    private Integer favorites;

    /** 对比次数 */
    private Integer compares;

    /** 提问数 */
    private Integer questions;

    /** 回答数 */
    private Integer answers;

    /** 发帖数 */
    private Integer posts;

    /** 评测数 */
    private Integer reviews;

    /** 选型搜索数 - DB列名selections */
    @TableField("selections")
    private Integer selectionSearches;

    /** 询价数 */
    private Integer inquiries;

    /** 采购需求数 */
    private Integer procurements;

    /** 品牌浏览数 */
    private Integer brandViews;

    /** 企业浏览数 */
    private Integer companyViews;

    /** 关注次数 */
    private Integer follows;

    /** 创建时间 */
    private java.time.LocalDateTime createTime;

    /** Phase10: 回访用户数 */
    private Integer returningUsers;
    /** Phase10: 订阅数 */
    private Integer subscriptionCount;
    /** Phase10: 通知发送数 */
    private Integer notificationSent;
    /** Phase10: 通知点击数 */
    private Integer notificationClicked;
    /** Phase10: 贡献用户数 */
    private Integer contributionUsers;
    /** Phase10: 被采纳纠错数 */
    private Integer acceptedCorrections;
    /** Phase10: 采购线索数 */
    private Integer procurementLeads;
    /** Phase10: 企业响应数 */
    private Integer enterpriseResponses;
    /** Phase10: 跟进转化数 */
    private Integer followConversions;
}