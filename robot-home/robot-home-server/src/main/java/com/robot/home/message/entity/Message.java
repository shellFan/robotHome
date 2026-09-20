package com.robot.home.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
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

    /** Phase10: 通知类型 */
    private String notificationType;
    /** Phase10: 目标类型 */
    private String targetType;
    /** Phase10: 目标ID */
    private Long targetId;
    /** Phase10: 摘要 */
    private String summary;
    /** Phase10: 事件键 */
    private String eventKey;
    /** Phase10: 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** Phase10: 点击追踪(CTR) 0未点击 1已点击 */
    private Integer clicked;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
