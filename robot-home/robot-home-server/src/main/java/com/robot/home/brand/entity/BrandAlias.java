package com.robot.home.brand.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

/**
 * 品牌别名（用于采集匹配）
 * 表名：brand_alias
 */
@Data
@TableName("brand_alias")
public class BrandAlias extends BaseEntity {

    /** 品牌ID */
    private Long brandId;
    /** 别名 */
    private String aliasName;
    /** 别名类型: NAME/ENGLISH/ABBREVIATION/OTHER */
    private String aliasType;
    /** 品牌名称(冗余) */
    private String brandName;
    /** 0停用 1启用 */
    private Integer isActive;
    /** 数据来源: DEMO/OFFICIAL/CRAWLER/MANUAL */
    private String dataSource;
}