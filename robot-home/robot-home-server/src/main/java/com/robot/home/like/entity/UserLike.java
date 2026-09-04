package com.robot.home.like.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通用点赞
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_like")
public class UserLike extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String bizType;
    private Long bizId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
