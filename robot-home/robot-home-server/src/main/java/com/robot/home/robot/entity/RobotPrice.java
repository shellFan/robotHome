package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 机器人价格
 * 表名：robot_price（无 deleted / create_time 列，单独实体）
 */
@Data
@TableName("robot_price")
public class RobotPrice {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long robotId;
    private String channel;
    private String region;
    private BigDecimal price;
    private LocalDateTime updateTime;
}
