package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerParamMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参数标准化映射 Mapper
 */
@Mapper
public interface CrawlerParamMappingMapper extends BaseMapper<CrawlerParamMapping> {
}