package com.robot.home.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.recommend.entity.RecommendItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐项 Mapper
 */
@Mapper
public interface RecommendItemMapper extends BaseMapper<com.robot.home.recommend.entity.RecommendItem> {
}
