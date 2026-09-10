package com.robot.home.feedback.controller;

import com.robot.home.common.Result;
import com.robot.home.feedback.dto.UserFeedbackDTO;
import com.robot.home.feedback.service.UserFeedbackService;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.common.util.SecurityUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户反馈 API（C端）
 */
@RestController
@RequestMapping("/api/feedback")
public class UserFeedbackController {

    @Resource
    private UserFeedbackService userFeedbackService;

    /**
     * 提交用户反馈
     * 限流：每IP每分钟5次
     */
    @PostMapping("/submit")
    @RateLimit(action = "feedback", windowSeconds = 60, maxRequests = 5, dimension = "IP")
    public Result<Long> submit(@RequestBody @Validated UserFeedbackDTO dto) {
        Long userId = SecurityUtils.currentUserId();
        Long id = userFeedbackService.submit(userId, dto);
        return Result.success(id);
    }
}