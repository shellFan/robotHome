package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerProductChange;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品参数变更记录 Mapper
 */
@Mapper
public interface CrawlerProductChangeMapper extends BaseMapper<CrawlerProductChange> {
}