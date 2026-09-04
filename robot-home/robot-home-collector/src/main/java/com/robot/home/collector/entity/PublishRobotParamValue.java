package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布到主站 robot_param_value 表的实体（采集器侧映射）
 */
@Data
@TableName("robot_param_value")
public class PublishRobotParamValue {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long robotId;
    private Long defId;
    private String value;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}