package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参数定义
 * 表名：robot_param_def（无 deleted / update_time 列，单独实体）
 */
@Data
@TableName("robot_param_def")
public class RobotParamDef {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long groupId;
    private String name;
    private String unit;
    private String type;
    private String options;
    private Integer sort;
    private Integer isCompare;
    private Integer isShow;
    private LocalDateTime createTime;
}
