package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerError;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集错误 Mapper
 */
@Mapper
public interface CrawlerErrorMapper extends BaseMapper<CrawlerError> {
}