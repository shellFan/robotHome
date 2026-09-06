package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.ArticleCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资讯栏目 Mapper（采集器侧，用于分类匹配）
 */
@Mapper
public interface ArticleCategoryMapper extends BaseMapper<ArticleCategory> {
}