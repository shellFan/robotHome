package com.robot.home.review.controller;

import cn.hutool.core.util.StrUtil;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.review.service.RobotReviewService;
import com.robot.home.review.vo.ReviewVO;
import com.robot.home.security.RequirePermission;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 后台：评价审核管理
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminRobotReviewController {

    @Resource
    private RobotReviewService reviewService;

    /**
     * 分页列表（可按状态/robotId筛选）
     */
    @GetMapping
    @RequirePermission("review:list")
    public Result<PageResult<ReviewVO>> page(@RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) Long robotId,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(reviewService.adminPage(status, robotId, pageNum, pageSize));
    }

    /**
     * 审核评价（通过/拒绝）
     * status: 1=通过 2=拒绝
     */
    @PostMapping("/{id}/audit")
    @RequirePermission("review:audit")
    public Result<Void> audit(@PathVariable Long id,
                               @RequestParam Integer status,
                               @RequestParam(required = false) String reason) {
        reviewService.audit(id, status, StrUtil.trim(reason));
        return Result.success();
    }

    /**
     * 官方回复
     */
    @PostMapping("/{id}/reply")
    @RequirePermission("review:reply")
    public Result<Void> reply(@PathVariable Long id,
                               @RequestParam String content) {
        reviewService.reply(id, content);
        return Result.success();
    }
}