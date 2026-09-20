package com.robot.home.message.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.message.service.NotificationService;
import com.robot.home.message.vo.MessageVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * Phase10: 通知中心2.0
 * 升级MessageController，增加类型过滤、软删除、按类型标记已读
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    /**
     * 通知列表（支持类型过滤）
     * GET /api/notifications?type=FOLLOW_UPDATE
     */
    @GetMapping
    public Result<PageResult<MessageVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(notificationService.listNotifications(userId, type, pageNum, pageSize));
    }

    /**
     * 未读数
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(Collections.singletonMap("count", notificationService.unreadCount(userId)));
    }

    /**
     * 标记已读
     */
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        notificationService.markRead(userId, id);
        return Result.success();
    }

    /**
     * 全部标记已读
     */
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = SecurityUtils.requireUserId();
        notificationService.markAllRead(userId);
        return Result.success();
    }

    /**
     * 按类型标记已读
     */
    @PostMapping("/read-by-type")
    public Result<Void> readByType(@RequestParam String type) {
        Long userId = SecurityUtils.requireUserId();
        notificationService.markReadByType(userId, type);
        return Result.success();
    }

    /**
     * 删除通知（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        notificationService.deleteNotification(userId, id);
        return Result.success();
    }
}