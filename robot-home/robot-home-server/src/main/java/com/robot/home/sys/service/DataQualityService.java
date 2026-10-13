package com.robot.home.sys.service;

import com.robot.home.sys.vo.CoverageDrillDownVO;
import com.robot.home.sys.vo.DataQualityVO;
import com.robot.home.sys.vo.DataScorecardVO;

/**
 * 数据质量统计服务
 * Phase12: 真实数据成绩报告
 */
public interface DataQualityService {
    DataQualityVO getDataQuality();

    /**
     * Phase12 P0-5: Coverage下钻统计
     * @param dimension 维度: dataSource/brand/category
     */
    CoverageDrillDownVO getCoverageDrillDown(String dimension);

    /**
     * Phase12 P0-6: 数据Scorecard — 捕获并持久化快照(不可变)
     * @param label 标签: BEFORE/AFTER
     */
    DataScorecardVO getScorecard(String label);

    /**
     * Phase12 P0-6: 从snapshot表读取最近一次指定label的快照(不可变)
     * @param label 标签: BEFORE/AFTER
     */
    DataScorecardVO getScorecardSnapshot(String label);
}