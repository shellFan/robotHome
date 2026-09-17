package com.robot.home.feedback.service.impl;

import cn.hutool.core.util.StrUtil;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.feedback.dto.UserFeedbackDTO;
import com.robot.home.feedback.entity.UserFeedback;
import com.robot.home.feedback.mapper.UserFeedbackMapper;
import com.robot.home.feedback.service.UserFeedbackService;
import com.robot.home.ratelimit.annotation.RateLimit;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 用户反馈服务实现
 */
@Service
public class UserFeedbackServiceImpl implements UserFeedbackService {

    @Resource
    private UserFeedbackMapper userFeedbackMapper;

    @Override
    public Long submit(Long userId, UserFeedbackDTO dto) {
        // 校验反馈类型
        if (!VALID_TYPES.contains(dto.getFeedbackType())) {
            throw new BusinessException("不支持的反馈类型: " + dto.getFeedbackType());
        }

        // XSS 防护：内容长度已由 @Size 限制
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setFeedbackType(dto.getFeedbackType());
        feedback.setContent(dto.getContent());
        feedback.setContact(StrUtil.isBlank(dto.getContact()) ? null : dto.getContact());
        feedback.setPageUrl(StrUtil.isBlank(dto.getPageUrl()) ? null : dto.getPageUrl());
        feedback.setStatus(0); // 待处理
        feedback.setCreateTime(new Date());
        feedback.setUpdateTime(new Date());
        userFeedbackMapper.insert(feedback);
        return feedback.getId();
    }
}