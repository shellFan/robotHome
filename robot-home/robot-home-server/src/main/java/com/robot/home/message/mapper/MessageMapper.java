package com.robot.home.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<com.robot.home.message.entity.Message> {
}
