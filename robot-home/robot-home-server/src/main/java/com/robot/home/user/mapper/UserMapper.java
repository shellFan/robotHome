package com.robot.home.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
