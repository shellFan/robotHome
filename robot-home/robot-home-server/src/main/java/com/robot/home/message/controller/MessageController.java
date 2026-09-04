package com.robot.home.message.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.message.service.MessageService;
import com.robot.home.message.vo.MessageVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * 站内消息
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Resource
    private MessageService messageService;

    @GetMapping
    public Result<PageResult<MessageVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(messageService.list(userId, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(Collections.singletonMap("count", messageService.unreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        messageService.markRead(userId, id);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = SecurityUtils.requireUserId();
        messageService.markAllRead(userId);
        return Result.success();
    }
}
