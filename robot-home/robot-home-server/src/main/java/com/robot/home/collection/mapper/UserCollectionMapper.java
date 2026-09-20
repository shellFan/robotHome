package com.robot.home.collection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collection.entity.UserCollection;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户收藏集 Mapper
 */
@Mapper
public interface UserCollectionMapper extends BaseMapper<UserCollection> {
}