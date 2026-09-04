package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerUrl;
import org.apache.ibatis.annotations.Mapper;

/**
 * URL队列 Mapper
 */
@Mapper
public interface CrawlerUrlMapper extends BaseMapper<CrawlerUrl> {
}