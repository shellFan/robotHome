package com.robot.home.like.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.like.entity.UserLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞 Mapper
 */
@Mapper
public interface UserLikeMapper extends BaseMapper<com.robot.home.like.entity.UserLike> {
}
