package com.robot.home.favorite.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.favorite.service.FavoriteService;
import com.robot.home.favorite.vo.FavoriteItemVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * 统一收藏
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    @GetMapping
    public Result<PageResult<FavoriteItemVO>> list(@RequestParam(required = false) String bizType,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(favoriteService.myFavorites(userId, bizType, pageNum, pageSize));
    }

    /**
     * 切换收藏状态
     */
    @PostMapping
    public Result<Map<String, Object>> toggle(@RequestParam String bizType, @RequestParam Long bizId) {
        Long userId = SecurityUtils.requireUserId();
        boolean favorited = favoriteService.toggle(userId, bizType, bizId);
        return Result.success(Collections.singletonMap("favorited", favorited));
    }

    @DeleteMapping("/{bizType}/{bizId}")
    public Result<Void> remove(@PathVariable String bizType, @PathVariable Long bizId) {
        Long userId = SecurityUtils.requireUserId();
        favoriteService.remove(userId, bizType, bizId);
        return Result.success();
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> check(@RequestParam String bizType, @RequestParam Long bizId) {
        Long userId = SecurityUtils.currentUserId();
        boolean favorited = userId != null && favoriteService.check(userId, bizType, bizId);
        return Result.success(Collections.singletonMap("favorited", favorited));
    }

    @GetMapping("/count")
    public Result<Map<String, Object>> count(@RequestParam(required = false) String bizType) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(Collections.singletonMap("count", favoriteService.count(userId, bizType)));
    }
}
