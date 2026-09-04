package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

/**
 * 机器人分类（可扩展，支持父级）
 * 表名：robot_category
 */
@Data
@TableName("robot_category")
public class RobotCategory extends BaseEntity {

    private Long parentId;
    private String name;
    private String icon;
    private Integer level;
    private Integer sort;
    private Integer status;
}
