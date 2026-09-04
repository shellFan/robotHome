package com.robot.home.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 站内消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class Message extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    /** system / reply / like / follow */
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
