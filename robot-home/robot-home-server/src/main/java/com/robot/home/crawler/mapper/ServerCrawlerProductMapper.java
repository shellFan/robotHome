package com.robot.home.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.crawler.entity.ServerCrawlerProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * Server端采集产品Mapper（轻量级，仅用于统计查询）
 * Phase12: 数据质量统计需要查询crawler_product表
 */
@Mapper
public interface ServerCrawlerProductMapper extends BaseMapper<ServerCrawlerProduct> {
}