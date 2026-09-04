package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发布到主站 robot_price 表的实体（采集器侧映射）
 */
@Data
@TableName("robot_price")
public class PublishRobotPrice {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long robotId;
    private String channel;
    private String region;
    private BigDecimal price;
    private LocalDateTime updateTime;
}