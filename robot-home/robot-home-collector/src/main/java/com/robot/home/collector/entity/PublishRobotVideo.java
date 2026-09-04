package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布到主站 robot_video 表的实体（采集器侧映射）
 */
@Data
@TableName("robot_video")
public class PublishRobotVideo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long robotId;
    private String title;
    private String url;
    private String cover;
    private Integer duration;
    private Integer sort;
    private LocalDateTime createTime;
}