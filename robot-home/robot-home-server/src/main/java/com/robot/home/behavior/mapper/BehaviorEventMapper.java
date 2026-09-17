package com.robot.home.behavior.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.behavior.entity.BehaviorEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行为事件 Mapper
 */
@Mapper
public interface BehaviorEventMapper extends BaseMapper<BehaviorEvent> {
}