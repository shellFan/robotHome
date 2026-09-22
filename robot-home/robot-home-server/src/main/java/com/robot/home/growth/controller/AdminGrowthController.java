package com.robot.home.growth.controller;

import com.robot.home.common.Result;
import com.robot.home.growth.service.GrowthService;
import com.robot.home.growth.vo.EcosystemDashboardVO;
import com.robot.home.growth.vo.GrowthDashboardVO;
import com.robot.home.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Phase9: Admin增长Dashboard
 * Phase10: 扩展生态Dashboard
 */
@RestController
@RequestMapping("/api/admin/growth")
public class AdminGrowthController {

    @Resource
    private GrowthService growthService;

    /**
     * 增长Dashboard（仅管理员可访问）
     */
    @GetMapping("/dashboard")
    @RequirePermission("growth:dashboard")
    public Result<GrowthDashboardVO> dashboard(
            @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(growthService.dashboard(days));
    }

    /**
     * P0-8: 生态Dashboard
     * 覆盖: 回访率、订阅、通知CTR、贡献、信誉、采购CRM、Pipeline转化
     */
    @GetMapping("/ecosystem")
    @RequirePermission("growth:dashboard")
    public Result<EcosystemDashboardVO> ecosystemDashboard(
            @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(growthService.ecosystemDashboard(days));
    }
}