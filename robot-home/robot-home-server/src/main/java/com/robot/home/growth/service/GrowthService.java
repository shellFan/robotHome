package com.robot.home.growth.service;

import com.robot.home.growth.vo.EcosystemDashboardVO;
import com.robot.home.growth.vo.GrowthDashboardVO;

/**
 * Phase9: 增长分析服务
 * Phase10: 扩展生态Dashboard
 */
public interface GrowthService {

    /**
     * 获取增长Dashboard数据
     * @param days 趋势天数（7或30）
     */
    GrowthDashboardVO dashboard(int days);

    /**
     * P0-8: 获取生态Dashboard数据
     * 覆盖Phase10新增指标: 回访率、订阅、通知CTR、贡献、信誉、采购CRM、转化
     * @param days 趋势天数（7或30）
     */
    EcosystemDashboardVO ecosystemDashboard(int days);
}