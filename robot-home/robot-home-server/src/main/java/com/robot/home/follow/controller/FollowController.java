package com.robot.home.follow.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.follow.service.FollowService;
import com.robot.home.follow.vo.FollowItemVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 关注：user / brand / company / robot
 */
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    /** followType白名单，防止任意字符串注入 */
    private static final Set<String> VALID_FOLLOW_TYPES = new HashSet<>(Arrays.asList("user", "brand", "company", "robot"));

    private static boolean isValidFollowType(String followType) {
        return followType != null && VALID_FOLLOW_TYPES.contains(followType.toLowerCase());
    }

    @Resource
    private FollowService followService;

    @GetMapping
    public Result<PageResult<FollowItemVO>> list(@RequestParam(required = false) String followType,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        if (followType != null && !isValidFollowType(followType)) {
            throw new BusinessException("无效的关注类型");
        }
        return Result.success(followService.myFollows(userId, followType, pageNum, pageSize));
    }

    @PostMapping
    public Result<Map<String, Object>> toggle(@RequestParam String followType, @RequestParam Long followId) {
        if (!isValidFollowType(followType)) {
            throw new BusinessException("无效的关注类型");
        }
        Long userId = SecurityUtils.requireUserId();
        boolean followed = followService.toggle(userId, followType.toLowerCase(), followId);
        return Result.success(Collections.singletonMap("followed", followed));
    }

    @DeleteMapping("/{followType}/{followId}")
    public Result<Void> remove(@PathVariable String followType, @PathVariable Long followId) {
        if (!isValidFollowType(followType)) {
            throw new BusinessException("无效的关注类型");
        }
        Long userId = SecurityUtils.requireUserId();
        followService.remove(userId, followType.toLowerCase(), followId);
        return Result.success();
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> check(@RequestParam String followType, @RequestParam Long followId) {
        if (!isValidFollowType(followType)) {
            throw new BusinessException("无效的关注类型");
        }
        Long userId = SecurityUtils.currentUserId();
        boolean followed = userId != null && followService.check(userId, followType.toLowerCase(), followId);
        return Result.success(Collections.singletonMap("followed", followed));
    }
}
