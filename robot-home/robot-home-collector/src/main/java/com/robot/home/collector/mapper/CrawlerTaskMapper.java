package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集任务 Mapper
 */
@Mapper
public interface CrawlerTaskMapper extends BaseMapper<CrawlerTask> {
}