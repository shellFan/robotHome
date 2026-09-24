package com.robot.home.quality.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.quality.entity.RobotQualityIssue;
import com.robot.home.quality.entity.RobotQualityScore;
import com.robot.home.quality.service.RobotQualityService;
import com.robot.home.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据质量API
 * 
 * 公开端点: /api/quality/score/{robotId}, /api/quality/issues/{robotId}
 * 管理端点: /api/admin/quality/** (AdminInterceptor自动保护)
 */
@RestController
@RequiredArgsConstructor
public class RobotQualityController {

    private final RobotQualityService qualityService;

    /** 获取机器人质量评分（公开） */
    @GetMapping("/api/quality/score/{robotId}")
    public RobotQualityScore getScore(@PathVariable Long robotId) {
        return qualityService.getScore(robotId);
    }

    /** 获取机器人质量问题（公开） */
    @GetMapping("/api/quality/issues/{robotId}")
    public List<RobotQualityIssue> getIssues(@PathVariable Long robotId) {
        return qualityService.getIssues(robotId);
    }

    // ===== 管理端端点: /api/admin/quality/** (AdminInterceptor自动保护) =====

    /** 管理端: 计算并更新单个机器人质量评分 */
    @PostMapping("/api/admin/quality/compute/{robotId}")
    @RequirePermission("quality:manage")
    public RobotQualityScore compute(@PathVariable Long robotId) {
        return qualityService.computeAndSave(robotId);
    }

    /** Phase11: 管理端: 批量计算所有机器人质量评分 */
    @PostMapping("/api/admin/quality/compute-all")
    @RequirePermission("quality:manage")
    public Result<Integer> computeAll() {
        int count = qualityService.computeAll();
        return Result.success(count);
    }

    /** 管理端: 分页查询质量评分 */
    @GetMapping("/api/admin/quality/scores")
    @RequirePermission("quality:view")
    public PageResult<RobotQualityScore> adminListScores(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return qualityService.adminListScores(pageNum, pageSize);
    }

    /** 管理端: 分页查询质量问题 */
    @GetMapping("/api/admin/quality/issues")
    @RequirePermission("quality:view")
    public PageResult<RobotQualityIssue> adminListIssues(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return qualityService.adminListIssues(pageNum, pageSize, status);
    }

    /** 管理端: 更新问题状态 */
    @PutMapping("/api/admin/quality/issues/{id}/status")
    @RequirePermission("quality:manage")
    public void updateIssueStatus(@PathVariable Long id, @RequestParam Integer status) {
        qualityService.updateIssueStatus(id, status);
    }
}