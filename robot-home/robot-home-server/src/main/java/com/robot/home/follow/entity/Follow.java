package com.robot.home.follow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 关注：user / brand / company / robot
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("follow")
public class Follow extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String followType;
    private Long followId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
