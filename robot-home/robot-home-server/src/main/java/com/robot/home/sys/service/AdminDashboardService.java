package com.robot.home.sys.service;

import com.robot.home.sys.vo.DashboardStatsVO;
import com.robot.home.sys.vo.DashboardTrendVO;

import java.util.List;

/**
 * 后台 Dashboard 统计服务
 */
public interface AdminDashboardService {

    DashboardStatsVO stats();

    /**
     * 近 N 天趋势
     */
    List<DashboardTrendVO> trend(int days);
}
