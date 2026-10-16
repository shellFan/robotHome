package com.robot.home.commercial.controller;

import com.robot.home.common.Result;
import com.robot.home.commercial.service.PlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 套餐接口（公开）
 */
@RestController
@RequestMapping("/api/plans")
public class EnterprisePlanController {

    @Resource
    private PlanService planService;

    /**
     * 在售套餐列表
     */
    @GetMapping
    public Result<?> listOnShelf() {
        return Result.success(planService.listOnShelfPlans());
    }

    /**
     * 套餐详情（含活跃版本权益配置）
     */
    @GetMapping("/{planId}")
    public Result<?> getPlanDetail(@PathVariable Long planId) {
        return Result.success(planService.getPlanDetail(planId));
    }
}