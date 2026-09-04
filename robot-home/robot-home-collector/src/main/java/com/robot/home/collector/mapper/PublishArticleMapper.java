package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.PublishArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发布文章 Mapper（写入主站 article 表）
 */
@Mapper
public interface PublishArticleMapper extends BaseMapper<PublishArticle> {
}