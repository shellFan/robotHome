package com.robot.home.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.favorite.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一收藏 Mapper
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<com.robot.home.favorite.entity.Favorite> {
}
