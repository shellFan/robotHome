package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerSource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集数据源 Mapper
 */
@Mapper
public interface CrawlerSourceMapper extends BaseMapper<CrawlerSource> {
}