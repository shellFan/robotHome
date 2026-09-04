package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机器人图片
 * 表名：robot_image（无 deleted / update_time 列，单独实体）
 */
@Data
@TableName("robot_image")
public class RobotImage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long robotId;
    private String url;
    private String type;
    private Integer sort;
    private LocalDateTime createTime;
}
