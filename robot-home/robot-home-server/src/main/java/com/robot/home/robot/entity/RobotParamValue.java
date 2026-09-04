package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机器人参数值
 * 表名：robot_param_value（无 deleted 列，单独实体）
 */
@Data
@TableName("robot_param_value")
public class RobotParamValue {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long robotId;
    private Long defId;
    private String value;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
