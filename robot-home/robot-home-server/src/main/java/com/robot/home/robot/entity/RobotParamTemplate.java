package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

/**
 * 参数模板（关联分类）
 * 表名：robot_param_template
 */
@Data
@TableName("robot_param_template")
public class RobotParamTemplate extends BaseEntity {

    private Long categoryId;
    private String name;
    private Integer sort;
    private Integer status;
}
