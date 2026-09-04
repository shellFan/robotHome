package com.robot.home.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.tutorial.entity.Tutorial;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教程 Mapper
 */
@Mapper
public interface TutorialMapper extends BaseMapper<com.robot.home.tutorial.entity.Tutorial> {
}
