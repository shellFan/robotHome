package com.robot.home.like.controller;

import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.like.service.LikeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * 通用点赞
 */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Resource
    private LikeService likeService;

    @PostMapping
    public Result<Map<String, Object>> toggle(@RequestParam String bizType, @RequestParam Long bizId) {
        Long userId = SecurityUtils.requireUserId();
        boolean liked = likeService.toggle(userId, bizType, bizId);
        return Result.success(Collections.singletonMap("liked", liked));
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> check(@RequestParam String bizType, @RequestParam Long bizId) {
        Long userId = SecurityUtils.currentUserId();
        boolean liked = userId != null && likeService.check(userId, bizType, bizId);
        return Result.success(Collections.singletonMap("liked", liked));
    }

    @GetMapping("/count")
    public Result<Map<String, Object>> count() {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(Collections.singletonMap("count", likeService.count(userId)));
    }
}
