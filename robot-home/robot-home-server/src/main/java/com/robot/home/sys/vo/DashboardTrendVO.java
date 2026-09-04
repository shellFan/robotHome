package com.robot.home.sys.vo;

import lombok.Data;

/**
 * 趋势图中的单日数据
 */
@Data
public class DashboardTrendVO {

    private String date;
    private Long pv;
    private Long uv;
    private Long newUsers;
    private Long inquiries;
}
