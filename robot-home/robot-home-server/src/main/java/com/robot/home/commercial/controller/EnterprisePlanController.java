package com.robot.home.commercial.controller;

import com.robot.home.common.Result;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterprisePlanVersion;
import com.robot.home.commercial.service.PlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 套餐接口（公开）
 */
@RestController
@RequestMapping("/api/plans")
public class EnterprisePlanController {

    @Resource
    private PlanService planService;

    /**
     * 在售套餐列表（含活跃版本价格与权益配置，便于前端展示）
     */
    @GetMapping
    public Result<?> listOnShelf() {
        List<EnterprisePlan> plans = planService.listOnShelfPlans();
        List<Map<String, Object>> result = new ArrayList<>();
        for (EnterprisePlan plan : plans) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", plan.getId());
            item.put("code", plan.getCode());
            item.put("name", plan.getName());
            item.put("description", plan.getDescription());
            item.put("level", plan.getLevel());
            item.put("displayPrice", plan.getDisplayPrice());
            item.put("durationDays", plan.getDurationDays());
            item.put("sort", plan.getSort());
            item.put("status", plan.getStatus());
            item.put("currentVersion", plan.getCurrentVersion());
            item.put("saleable", plan.getSaleable());
            // 活跃版本价格与权益配置（前端套餐卡片展示需要）
            EnterprisePlanVersion version = planService.getActiveVersion(plan.getId());
            if (version != null) {
                item.put("priceCents", version.getPriceCents());
                item.put("entitlementConfig", version.getEntitlementConfig());
                item.put("versionDurationDays", version.getDurationDays());
            } else {
                item.put("priceCents", null);
                item.put("entitlementConfig", "[]");
                item.put("versionDurationDays", null);
            }
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 套餐详情（含活跃版本权益配置）
     */
    @GetMapping("/{planId}")
    public Result<?> getPlanDetail(@PathVariable Long planId) {
        return Result.success(planService.getPlanDetail(planId));
    }
}