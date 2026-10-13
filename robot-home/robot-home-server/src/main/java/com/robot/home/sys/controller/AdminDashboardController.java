package com.robot.home.sys.controller;

import com.robot.home.common.Result;
import com.robot.home.crawler.CrawlerProxyController;
import com.robot.home.quality.service.RobotQualityService;
import com.robot.home.robot.service.RobotService;
import com.robot.home.security.RequirePermission;
import com.robot.home.sys.service.AdminDashboardService;
import com.robot.home.sys.service.DataQualityService;
import com.robot.home.sys.vo.CoverageDrillDownVO;
import com.robot.home.sys.vo.DashboardStatsVO;
import com.robot.home.sys.vo.DashboardTrendVO;
import com.robot.home.sys.vo.DataQualityVO;
import com.robot.home.sys.vo.DataScorecardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Resource
    private DataQualityService dataQualityService;

    @Resource
    private RobotService robotService;

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

    /** Phase12: 数据质量详细统计 — Coverage/Freshness/Source Health */
    @RequirePermission("dashboard:view")
    @GetMapping("/data-quality")
    public Result<DataQualityVO> dataQuality() {
        return Result.success(dataQualityService.getDataQuality());
    }

    /** Phase12: 刷新所有Robot的Freshness新鲜度 */
    @RequirePermission("dashboard:view")
    @PostMapping("/refresh-freshness")
    public Result<Map<String, Object>> refreshFreshness() {
        int updated = robotService.refreshAllFreshness();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated);
        return Result.success(result);
    }

    /** Phase12 P0-5: Coverage下钻统计 — 按dataSource/brand/category维度 */
    @RequirePermission("dashboard:view")
    @GetMapping("/coverage-drill-down")
    public Result<CoverageDrillDownVO> coverageDrillDown(
            @RequestParam(defaultValue = "dataSource") String dimension) {
        try {
            return Result.success(dataQualityService.getCoverageDrillDown(dimension));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** Phase12 P0-6: 数据Scorecard快照 — 捕获并持久化(不可变) */
    @RequirePermission("dashboard:view")
    @PostMapping("/scorecard")
    public Result<DataScorecardVO> captureScorecard(
            @RequestParam(defaultValue = "SNAPSHOT") String label) {
        return Result.success(dataQualityService.getScorecard(label));
    }

    /** Phase12 P0-6: 读取最近一次指定label的Scorecard快照(不可变) */
    @RequirePermission("dashboard:view")
    @GetMapping("/scorecard")
    public Result<DataScorecardVO> getScorecard(
            @RequestParam(defaultValue = "BEFORE") String label) {
        DataScorecardVO snapshot = dataQualityService.getScorecardSnapshot(label);
        if (snapshot == null) {
            return Result.success(null);
        }
        return Result.success(snapshot);
    }
}
