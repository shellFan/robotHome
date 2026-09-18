package com.robot.home.growth.service;

import com.robot.home.growth.vo.GrowthDashboardVO;

/**
 * Phase9: 增长分析服务
 */
public interface GrowthService {

    /**
     * 获取增长Dashboard数据
     * @param days 趋势天数（7或30）
     */
    GrowthDashboardVO dashboard(int days);
}