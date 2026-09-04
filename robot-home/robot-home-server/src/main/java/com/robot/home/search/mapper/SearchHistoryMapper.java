package com.robot.home.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.search.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索历史 Mapper
 */
@Mapper
public interface SearchHistoryMapper extends BaseMapper<com.robot.home.search.entity.SearchHistory> {
}
