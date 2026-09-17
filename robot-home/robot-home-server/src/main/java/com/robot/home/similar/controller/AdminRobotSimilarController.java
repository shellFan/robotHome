package com.robot.home.similar.controller;

import com.robot.home.common.Result;
import com.robot.home.security.RequirePermission;
import com.robot.home.similar.service.RobotSimilarService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 后台：相似度管理
 */
@RestController
@RequestMapping("/api/admin/similar")
public class AdminRobotSimilarController {

    @Resource
    private RobotSimilarService similarService;

    /**
     * 重算指定机器人的相似度
     */
    @PostMapping("/recalculate")
    @RequirePermission("similar:recalculate")
    public Result<Void> recalculate(@RequestParam Long robotId) {
        similarService.recalculate(robotId);
        return Result.success();
    }

    /**
     * 全量重算所有机器人相似度
     */
    @PostMapping("/recalculate-all")
    @RequirePermission("similar:recalculate")
    public Result<Void> recalculateAll() {
        similarService.recalculateAll();
        return Result.success();
    }
}