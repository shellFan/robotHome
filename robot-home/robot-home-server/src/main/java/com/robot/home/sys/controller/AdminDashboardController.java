package com.robot.home.sys.controller;

import com.robot.home.common.Result;
import com.robot.home.security.RequirePermission;
import com.robot.home.sys.service.AdminDashboardService;
import com.robot.home.sys.vo.DashboardStatsVO;
import com.robot.home.sys.vo.DashboardTrendVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 后台 Dashboard
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Resource
    private AdminDashboardService dashboardService;

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
}
