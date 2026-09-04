package com.robot.home.follow.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.follow.service.FollowService;
import com.robot.home.follow.vo.FollowItemVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * 关注：user / brand / company / robot
 */
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    @Resource
    private FollowService followService;

    @GetMapping
    public Result<PageResult<FollowItemVO>> list(@RequestParam(required = false) String followType,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(followService.myFollows(userId, followType, pageNum, pageSize));
    }

    @PostMapping
    public Result<Map<String, Object>> toggle(@RequestParam String followType, @RequestParam Long followId) {
        Long userId = SecurityUtils.requireUserId();
        boolean followed = followService.toggle(userId, followType, followId);
        return Result.success(Collections.singletonMap("followed", followed));
    }

    @DeleteMapping("/{followType}/{followId}")
    public Result<Void> remove(@PathVariable String followType, @PathVariable Long followId) {
        Long userId = SecurityUtils.requireUserId();
        followService.remove(userId, followType, followId);
        return Result.success();
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> check(@RequestParam String followType, @RequestParam Long followId) {
        Long userId = SecurityUtils.currentUserId();
        boolean followed = userId != null && followService.check(userId, followType, followId);
        return Result.success(Collections.singletonMap("followed", followed));
    }
}
