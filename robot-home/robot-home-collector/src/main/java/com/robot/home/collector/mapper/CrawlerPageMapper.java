package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerPage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 页面原始内容 Mapper
 */
@Mapper
public interface CrawlerPageMapper extends BaseMapper<CrawlerPage> {
}