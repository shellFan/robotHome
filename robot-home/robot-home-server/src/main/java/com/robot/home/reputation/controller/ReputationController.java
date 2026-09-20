package com.robot.home.reputation.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.reputation.service.ReputationService;
import com.robot.home.reputation.vo.ReputationEventVO;
import com.robot.home.reputation.vo.ReputationVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * P0-7: 信誉公开接口
 * - GET /api/users/{userId}/reputation — 查看用户信誉
 * - GET /api/users/me/reputation — 我的信誉
 * - GET /api/users/me/reputation/events — 我的信誉事件
 */
@RestController
@RequestMapping("/api/users")
public class ReputationController {

    @Resource
    private ReputationService reputationService;

    /**
     * 查看用户信誉（公开）
     */
    @GetMapping("/{userId}/reputation")
    public Result<ReputationVO> getReputation(@PathVariable Long userId) {
        return Result.success(reputationService.getReputation(userId));
    }

    /**
     * 我的信誉
     */
    @GetMapping("/me/reputation")
    public Result<ReputationVO> myReputation() {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(reputationService.getReputation(userId));
    }

    /**
     * 我的信誉事件历史
     */
    @GetMapping("/me/reputation/events")
    public Result<PageResult<ReputationEventVO>> myEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(reputationService.getEventHistory(userId, eventType, pageNum, pageSize));
    }
}