package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

/**
 * 参数标准化映射
 * 表名：param_mapping
 */
@Data
@TableName("param_mapping")
public class ParamMapping extends BaseEntity {

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
    /** 0停用 1启用 */
    private Integer isActive;
}