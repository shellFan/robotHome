package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.PublishRobot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发布机器人 Mapper（写入主站 robot 表）
 */
@Mapper
public interface PublishRobotMapper extends BaseMapper<PublishRobot> {
}