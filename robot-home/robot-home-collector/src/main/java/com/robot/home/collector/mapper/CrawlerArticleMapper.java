package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集文章 Mapper
 */
@Mapper
public interface CrawlerArticleMapper extends BaseMapper<CrawlerArticle> {
}