package com.robot.home.ratelimit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.ratelimit.entity.RateLimitConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RateLimitConfigMapper extends BaseMapper<RateLimitConfig> {
}