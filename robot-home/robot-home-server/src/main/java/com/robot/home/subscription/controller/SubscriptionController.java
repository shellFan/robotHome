package com.robot.home.subscription.controller;

import cn.hutool.core.util.StrUtil;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.subscription.service.SubscriptionService;
import com.robot.home.subscription.vo.SubscriptionVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 订阅接口：ROBOT / BRAND / COMPANY
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    /** targetType白名单 */
    private static final Set<String> VALID_TARGET_TYPES = new HashSet<>(Arrays.asList(
            Constants.SUB_TARGET_ROBOT, Constants.SUB_TARGET_BRAND, Constants.SUB_TARGET_COMPANY));

    private static boolean isValidTargetType(String targetType) {
        return targetType != null && VALID_TARGET_TYPES.contains(targetType.toUpperCase());
    }

    @Resource
    private SubscriptionService subscriptionService;

    /**
     * 切换订阅
     */
    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggle(@RequestParam String targetType, @RequestParam Long targetId) {
        if (!isValidTargetType(targetType)) {
            throw new BusinessException("无效的订阅目标类型");
        }
        Long userId = SecurityUtils.requireUserId();
        boolean subscribed = subscriptionService.toggle(userId, targetType.toUpperCase(), targetId);
        return Result.success(Collections.singletonMap("subscribed", subscribed));
    }

    /**
     * 更新订阅事件类型
     */
    @PutMapping("/{id}/event-types")
    public Result<Void> updateEventTypes(@PathVariable Long id, @RequestParam String eventTypes) {
        Long userId = SecurityUtils.requireUserId();
        // 查询订阅记录，IDOR防护：确保是当前用户的订阅
        com.robot.home.subscription.entity.UserSubscription sub =
                subscriptionService.getById(id);
        if (sub == null) {
            throw new BusinessException("订阅记录不存在");
        }
        if (!sub.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订阅记录");
        }
        // XSS清洗
        String safeEventTypes = XssUtils.escapeText(StrUtil.trim(eventTypes));
        subscriptionService.updateEventTypes(userId, sub.getTargetType(), sub.getTargetId(), safeEventTypes);
        return Result.success();
    }

    /**
     * 我的订阅列表
     */
    @GetMapping
    public Result<PageResult<SubscriptionVO>> list(@RequestParam(required = false) String targetType,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        if (targetType != null && !isValidTargetType(targetType)) {
            throw new BusinessException("无效的订阅目标类型");
        }
        return Result.success(subscriptionService.mySubscriptions(userId,
                targetType != null ? targetType.toUpperCase() : null, pageNum, pageSize));
    }

    /**
     * 我的订阅数
     */
    @GetMapping("/count")
    public Result<Map<String, Object>> count(@RequestParam(required = false) String targetType) {
        Long userId = SecurityUtils.requireUserId();
        if (targetType != null && !isValidTargetType(targetType)) {
            throw new BusinessException("无效的订阅目标类型");
        }
        long total = subscriptionService.count(userId,
                targetType != null ? targetType.toUpperCase() : null);
        return Result.success(Collections.singletonMap("count", total));
    }

    /**
     * 检查是否订阅
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> check(@RequestParam String targetType, @RequestParam Long targetId) {
        if (!isValidTargetType(targetType)) {
            throw new BusinessException("无效的订阅目标类型");
        }
        Long userId = SecurityUtils.currentUserId();
        boolean subscribed = userId != null && subscriptionService.checkSubscribed(userId, targetType.toUpperCase(), targetId);
        return Result.success(Collections.singletonMap("subscribed", subscribed));
    }
}