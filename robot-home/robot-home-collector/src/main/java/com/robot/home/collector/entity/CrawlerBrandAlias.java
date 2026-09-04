package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌别名(用于匹配)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_brand_alias")
public class CrawlerBrandAlias extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 品牌ID */
    private Long brandId;

    /** 别名 */
    private String aliasName;

    /** 别名类型: NAME/ENGLISH/ABBREVIATION/OTHER */
    private String aliasType;

    /** 品牌名称(冗余，用于快速查询) */
    private String brandName;

    /** 是否启用: 0停用 1启用 */
    private Integer isActive;
}