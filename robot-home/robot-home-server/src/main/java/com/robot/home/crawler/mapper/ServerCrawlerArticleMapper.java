package com.robot.home.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.crawler.entity.ServerCrawlerArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * Server端采集文章Mapper（轻量级，仅用于统计查询）
 * Phase12: 数据质量统计需要查询crawler_article表
 */
@Mapper
public interface ServerCrawlerArticleMapper extends BaseMapper<ServerCrawlerArticle> {
}