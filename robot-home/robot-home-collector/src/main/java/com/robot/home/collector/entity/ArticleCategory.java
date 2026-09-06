package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 资讯栏目（article_category 表，采集器侧映射）
 * 用于 resolveArticleCategory 分类匹配
 */
@Data
@TableName("article_category")
public class ArticleCategory {

    private Long id;

    /** 栏目名称 */
    private String name;

    private Integer sort;

    /** 0禁用 1正常 */
    private Integer status;
}