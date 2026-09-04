package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数标准化映射
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_param_mapping")
public class CrawlerParamMapping extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 标准化名称 */
    private String normalizedName;

    /** 原始名称 */
    private String rawName;

    /** 机器人分类 */
    private String category;

    /** 单位 */
    private String unit;

    /** 别名列表(逗号分隔) */
    private String aliases;

    /** 是否启用: 0停用 1启用 */
    private Integer isActive;
}