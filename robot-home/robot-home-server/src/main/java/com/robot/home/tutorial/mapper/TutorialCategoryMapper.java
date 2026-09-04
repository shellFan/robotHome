package com.robot.home.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.tutorial.entity.TutorialCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教程分类 Mapper
 */
@Mapper
public interface TutorialCategoryMapper extends BaseMapper<com.robot.home.tutorial.entity.TutorialCategory> {
}
