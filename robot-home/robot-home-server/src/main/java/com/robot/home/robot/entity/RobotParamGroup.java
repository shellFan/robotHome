package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参数分组
 * 表名：robot_param_group（无 deleted / update_time 列，单独实体）
 */
@Data
@TableName("robot_param_group")
public class RobotParamGroup {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long templateId;
    private String name;
    private Integer sort;
    private LocalDateTime createTime;
}
