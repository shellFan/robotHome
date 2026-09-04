package com.robot.home.history.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.history.entity.BrowseHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 浏览历史 Mapper
 */
@Mapper
public interface BrowseHistoryMapper extends BaseMapper<com.robot.home.history.entity.BrowseHistory> {
}
