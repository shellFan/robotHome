package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布到主站 robot_tag 表的实体（采集器侧映射）
 */
@Data
@TableName("robot_tag")
public class PublishRobotTag {

    private Long id;
    private Long robotId;
    private String tagType;
    private String tagValue;
    private LocalDateTime createTime;
}