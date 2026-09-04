package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集日志 Mapper
 */
@Mapper
public interface CrawlerLogMapper extends BaseMapper<CrawlerLog> {
}