package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

/**
 * 机器人系列
 * 表名：robot_series
 */
@Data
@TableName("robot_series")
public class RobotSeries extends BaseEntity {

    private Long brandId;
    private Long categoryId;
    private String name;
    private Integer sort;
    private Integer status;
}
