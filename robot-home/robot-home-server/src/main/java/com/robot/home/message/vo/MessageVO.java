package com.robot.home.message.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息/通知视图
 * Phase10增强：通知类型、目标链接、摘要
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageVO {

    private Long id;
    private String type;
    /** Phase10: 通知类型 */
    private String notificationType;
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;
    private LocalDateTime createTime;
    /** Phase10: 目标类型(robot/brand/company/post/question/answer/review/procurement) */
    private String targetType;
    /** Phase10: 目标ID */
    private Long targetId;
    /** Phase10: 通知摘要 */
    private String summary;
}
