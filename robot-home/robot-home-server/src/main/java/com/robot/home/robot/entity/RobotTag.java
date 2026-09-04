package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 机器人标签：scene 使用场景 / dev 开发能力 / ai 人工智能 / feature 特性
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_tag")
public class RobotTag extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long robotId;
    private String tagType;
    private String tagValue;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
