package com.robot.home.article.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资讯栏目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article_category")
public class ArticleCategory extends BaseEntity {

    private String name;
    private Integer sort;
    private Integer status;
}
