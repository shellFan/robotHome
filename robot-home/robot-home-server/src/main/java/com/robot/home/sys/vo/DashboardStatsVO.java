package com.robot.home.sys.vo;

import lombok.Data;

/**
 * 后台首页统计
 */
@Data
public class DashboardStatsVO {

    private Long userCount;
    private Long todayNewUserCount;
    private Long robotCount;
    private Long brandCount;
    private Long companyCount;
    private Long articleCount;
    private Long videoCount;
    private Long tutorialCount;
    private Long postCount;
    private Long inquiryCount;
    private Long pendingInquiryCount;
    private Long commentCount;
    /** 今日 PV / UV */
    private Long todayPv;
    private Long todayUv;
}
