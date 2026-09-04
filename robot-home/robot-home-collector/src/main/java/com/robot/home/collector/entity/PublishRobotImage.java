package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布到主站 robot_image 表的实体（采集器侧映射）
 */
@Data
@TableName("robot_image")
public class PublishRobotImage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long robotId;
    private String url;
    private String type;
    private Integer sort;
    private LocalDateTime createTime;
}