package com.robot.home.subscription.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.subscription.entity.UserSubscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户订阅 Mapper
 */
@Mapper
public interface UserSubscriptionMapper extends BaseMapper<UserSubscription> {
}