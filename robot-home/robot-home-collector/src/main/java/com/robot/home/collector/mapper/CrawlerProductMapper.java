package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集产品 Mapper
 */
@Mapper
public interface CrawlerProductMapper extends BaseMapper<CrawlerProduct> {
}