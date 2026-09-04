package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布到主站 robot_param_def 表的实体（采集器侧映射，仅查询用）
 */
@Data
@TableName("robot_param_def")
public class PublishRobotParamDef {

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