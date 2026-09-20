package com.robot.home.topic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.topic.entity.Topic;
import org.apache.ibatis.annotations.Mapper;

/**
 * 话题 Mapper
 */
@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
}