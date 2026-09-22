package com.robot.home.reputation.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.XssUtils;
import com.robot.home.reputation.service.ReputationService;
import com.robot.home.reputation.vo.ReputationVO;
import com.robot.home.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * P0-7: 管理端信誉接口
 * - GET /api/admin/reputation/list — 按等级筛选用户
 * - POST /api/admin/reputation/{userId}/adjust — 手动调整信誉分
 * - POST /api/admin/reputation/{userId}/recalculate — 重算信誉分
 */
@RestController
@RequestMapping("/api/admin/reputation")
public class AdminReputationController {

    @Resource
    private ReputationService reputationService;

    /**
     * 按等级筛选用户信誉列表
     */
    @GetMapping("/list")
    @RequirePermission("reputation:list")
    public Result<PageResult<ReputationVO>> list(
            @RequestParam(required = false) String reputationLevel,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(reputationService.adminListByLevel(reputationLevel, pageNum, pageSize));
    }

    /**
     * 手动调整信誉分
     */
    @PostMapping("/{userId}/adjust")
    @RequirePermission("reputation:adjust")
    public Result<Void> adjustScore(
            @PathVariable Long userId,
            @RequestParam Integer delta,
            @RequestParam String reason) {
        // XSS防护
        String safeReason = XssUtils.escapeText(reason);
        reputationService.adminAdjustScore(userId, delta, safeReason);
        return Result.success(null);
    }

    /**
     * 重算用户信誉分
     */
    @PostMapping("/{userId}/recalculate")
    @RequirePermission("reputation:adjust")
    public Result<Void> recalculate(@PathVariable Long userId) {
        reputationService.recalculate(userId);
        return Result.success(null);
    }

    /**
     * 批量重算（谨慎使用）
     */
    @PostMapping("/batch-recalculate")
    @RequirePermission("reputation:adjust")
    public Result<Void> batchRecalculate(@RequestParam Long[] userIds) {
        // 限制批量数量，防止滥用
        if (userIds == null || userIds.length == 0) {
            return Result.success(null);
        }
        if (userIds.length > 100) {
            return Result.fail("单次批量重算不超过100个用户");
        }
        reputationService.batchRecalculate(java.util.Arrays.asList(userIds));
        return Result.success(null);
    }
}