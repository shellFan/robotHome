package com.robot.home.feedback.service;

import com.robot.home.feedback.dto.UserFeedbackDTO;
import com.robot.home.feedback.entity.UserFeedback;

import java.util.Arrays;
import java.util.List;

/**
 * 用户反馈服务
 */
public interface UserFeedbackService {

    /**
     * 提交反馈
     */
    Long submit(Long userId, UserFeedbackDTO dto);

    /**
     * 合法的反馈类型
     */
    List<String> VALID_TYPES = Arrays.asList("bug", "feature", "improvement", "other");
}