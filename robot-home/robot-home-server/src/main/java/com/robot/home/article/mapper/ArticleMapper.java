package com.robot.home.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.article.entity.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资讯文章 Mapper
 */
@Mapper
public interface ArticleMapper extends BaseMapper<com.robot.home.article.entity.Article> {
}
