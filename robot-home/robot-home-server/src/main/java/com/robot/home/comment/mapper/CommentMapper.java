package com.robot.home.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.comment.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通用评论 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<com.robot.home.comment.entity.Comment> {
}
