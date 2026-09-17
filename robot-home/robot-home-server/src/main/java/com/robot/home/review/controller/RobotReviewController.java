package com.robot.home.review.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.review.dto.ReviewDTO;
import com.robot.home.review.service.RobotReviewService;
import com.robot.home.review.vo.ReviewSummaryVO;
import com.robot.home.review.vo.ReviewVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

/**
 * C端：机器人评价
 */
@RestController
@RequestMapping("/api/reviews")
public class RobotReviewController {

    @Resource
    private RobotReviewService reviewService;

    /**
     * 提交评价
     */
    @PostMapping
    @RateLimit(action = "review", windowSeconds = 60, maxRequests = 3, dimension = "IP_USER")
    public Result<Map<String, Object>> submit(@RequestBody @Valid ReviewDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        Long id = reviewService.submit(userId, dto);
        return Result.success(Collections.singletonMap("id", id));
    }

    /**
     * 修改评价
     */
    @PutMapping("/{id}")
    @RateLimit(action = "review", windowSeconds = 60, maxRequests = 3, dimension = "IP_USER")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ReviewDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        reviewService.update(userId, id, dto);
        return Result.success();
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{id}")
    @RateLimit(action = "review_delete", windowSeconds = 60, maxRequests = 10, dimension = "IP_USER")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        reviewService.delete(userId, id);
        return Result.success();
    }

    /**
     * 某机器人的评价列表（仅已通过）
     */
    @GetMapping("/robot/{robotId}")
    public Result<PageResult<ReviewVO>> list(@PathVariable Long robotId,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Long currentUserId = SecurityUtils.currentUserId();
        return Result.success(reviewService.list(robotId, currentUserId, pageNum, pageSize));
    }

    /**
     * 某机器人的评价汇总
     */
    @GetMapping("/robot/{robotId}/summary")
    public Result<ReviewSummaryVO> summary(@PathVariable Long robotId) {
        return Result.success(reviewService.summary(robotId));
    }

    /**
     * 标记有用
     */
    @PostMapping("/{id}/helpful")
    @RateLimit(action = "review_helpful", windowSeconds = 10, maxRequests = 5, dimension = "IP_USER")
    public Result<Void> helpful(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        reviewService.helpful(userId, id);
        return Result.success();
    }

    /**
     * 取消有用
     */
    @DeleteMapping("/{id}/helpful")
    @RateLimit(action = "review_helpful", windowSeconds = 10, maxRequests = 5, dimension = "IP_USER")
    public Result<Void> unhelpful(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        reviewService.unhelpful(userId, id);
        return Result.success();
    }
}