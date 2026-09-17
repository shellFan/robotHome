package com.robot.home.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.feedback.entity.UserFeedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserFeedbackMapper extends BaseMapper<UserFeedback> {
}