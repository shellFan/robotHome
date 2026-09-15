package com.robot.home.search.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 搜索别名
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("search_alias")
public class SearchAlias extends BaseEntity {

    private String alias;
    /** 目标类型: robot/brand/company */
    private String targetType;
    private Long targetId;
    private String targetName;
    /** 1启用 0停用 */
    private Integer status;
}