package com.robot.home.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.crawler.entity.ServerCrawlerSource;
import org.apache.ibatis.annotations.Mapper;

/**
 * Server端采集源Mapper（轻量级，仅用于统计查询）
 * Phase12: 数据质量统计 + Source健康度
 */
@Mapper
public interface ServerCrawlerSourceMapper extends BaseMapper<ServerCrawlerSource> {
}