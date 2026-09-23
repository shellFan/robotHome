package com.robot.home.discovery.vo;

import lombok.Data;

/**
 * 平台统计摘要（发现页用）
 */
@Data
public class DiscoveryStatsVO {

    /** 机器人总数 */
    private Integer robotCount;
    /** 品牌总数 */
    private Integer brandCount;
    /** 企业总数 */
    private Integer companyCount;
    /** 社区帖子数 */
    private Integer postCount;
    /** 问答数 */
    private Integer questionCount;
    /** 今日新增机器人 */
    private Integer todayNewRobots;
    /** 今日活跃用户 */
    private Integer todayActiveUsers;
}