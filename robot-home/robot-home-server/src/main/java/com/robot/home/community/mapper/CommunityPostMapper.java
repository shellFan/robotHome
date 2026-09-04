package com.robot.home.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.community.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社区帖子 Mapper
 */
@Mapper
public interface CommunityPostMapper extends BaseMapper<com.robot.home.community.entity.CommunityPost> {
}
