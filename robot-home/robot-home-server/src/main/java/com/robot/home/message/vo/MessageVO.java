package com.robot.home.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息视图
 */
@Data
public class MessageVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;
    private LocalDateTime createTime;
}
