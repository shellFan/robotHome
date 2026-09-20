package com.robot.home.reputation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.reputation.entity.UserReputation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信誉 Mapper
 */
@Mapper
public interface UserReputationMapper extends BaseMapper<UserReputation> {
}