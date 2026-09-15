package com.robot.home.quality.controller;

import com.robot.home.common.PageResult;
import com.robot.home.quality.entity.RobotQualityIssue;
import com.robot.home.quality.entity.RobotQualityScore;
import com.robot.home.quality.service.RobotQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据质量API
 */
@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class RobotQualityController {

    private final RobotQualityService qualityService;

    /** 获取机器人质量评分 */
    @GetMapping("/score/{robotId}")
    public RobotQualityScore getScore(@PathVariable Long robotId) {
        return qualityService.getScore(robotId);
    }

    /** 获取机器人质量问题 */
    @GetMapping("/issues/{robotId}")
    public List<RobotQualityIssue> getIssues(@PathVariable Long robotId) {
        return qualityService.getIssues(robotId);
    }

    /** 管理端: 计算并更新质量评分 */
    @PostMapping("/admin/compute/{robotId}")
    public RobotQualityScore compute(@PathVariable Long robotId) {
        return qualityService.computeAndSave(robotId);
    }

    /** 管理端: 分页查询质量评分 */
    @GetMapping("/admin/scores")
    public PageResult<RobotQualityScore> adminListScores(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return qualityService.adminListScores(pageNum, pageSize);
    }

    /** 管理端: 分页查询质量问题 */
    @GetMapping("/admin/issues")
    public PageResult<RobotQualityIssue> adminListIssues(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return qualityService.adminListIssues(pageNum, pageSize, status);
    }

    /** 管理端: 更新问题状态 */
    @PutMapping("/admin/issues/{id}/status")
    public void updateIssueStatus(@PathVariable Long id, @RequestParam Integer status) {
        qualityService.updateIssueStatus(id, status);
    }
}