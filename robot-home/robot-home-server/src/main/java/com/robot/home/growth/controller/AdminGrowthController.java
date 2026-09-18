package com.robot.home.growth.controller;

import com.robot.home.common.Result;
import com.robot.home.growth.service.GrowthService;
import com.robot.home.growth.vo.GrowthDashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Phase9: Admin增长Dashboard
 */
@RestController
@RequestMapping("/api/admin/growth")
public class AdminGrowthController {

    @Resource
    private GrowthService growthService;

    /**
     * 增长Dashboard
     */
    @GetMapping("/dashboard")
    public Result<GrowthDashboardVO> dashboard(
            @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(growthService.dashboard(days));
    }
}