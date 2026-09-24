package com.robot.home.sys.controller;

import com.robot.home.common.Result;
import com.robot.home.crawler.CrawlerProxyController;
import com.robot.home.quality.service.RobotQualityService;
import com.robot.home.security.RequirePermission;
import com.robot.home.sys.service.AdminDashboardService;
import com.robot.home.sys.vo.DashboardStatsVO;
import com.robot.home.sys.vo.DashboardTrendVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台 Dashboard
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Resource
    private AdminDashboardService dashboardService;

    @Resource
    private CrawlerProxyController crawlerProxyController;

    @Resource
    private RobotQualityService qualityService;

    @RequirePermission("dashboard:view")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(dashboardService.stats());
    }

    @RequirePermission("dashboard:view")
    @GetMapping("/trend")
    public Result<List<DashboardTrendVO>> trend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(dashboardService.trend(days == null ? 7 : days));
    }

    /** Phase11: 采集器健康状态 */
    @RequirePermission("dashboard:view")
    @GetMapping("/crawler-health")
    public Result<Map<String, Object>> crawlerHealth() {
        return crawlerProxyController.health();
    }

    /** Phase11: 数据质量概览 */
    @RequirePermission("dashboard:view")
    @GetMapping("/quality-summary")
    public Result<Map<String, Object>> qualitySummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        // 获取低分机器人数量(总分<50)
        int lowScoreCount = qualityService.countLowScoreRobots(50);
        int totalScored = qualityService.countScoredRobots();
        int pendingIssues = qualityService.countPendingIssues();
        summary.put("totalScored", totalScored);
        summary.put("lowScoreCount", lowScoreCount);
        summary.put("pendingIssues", pendingIssues);
        return Result.success(summary);
    }
}
