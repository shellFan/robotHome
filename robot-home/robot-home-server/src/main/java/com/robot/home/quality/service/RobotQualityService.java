package com.robot.home.quality.service;

import com.robot.home.common.PageResult;
import com.robot.home.quality.entity.RobotQualityIssue;
import com.robot.home.quality.entity.RobotQualityScore;

import java.util.List;

/**
 * 数据质量服务
 */
public interface RobotQualityService {

    /** 计算并更新机器人质量评分 */
    RobotQualityScore computeAndSave(Long robotId);

    /** 获取质量评分 */
    RobotQualityScore getScore(Long robotId);

    /** 获取质量问题列表 */
    List<RobotQualityIssue> getIssues(Long robotId);

    /** 管理端: 分页查询质量评分 */
    PageResult<RobotQualityScore> adminListScores(Integer pageNum, Integer pageSize);

    /** 管理端: 分页查询质量问题 */
    PageResult<RobotQualityIssue> adminListIssues(Integer pageNum, Integer pageSize, Integer status);

    /** 管理端: 更新问题状态 */
    void updateIssueStatus(Long id, Integer status);
}