package com.robot.home.qa.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回答视图
 */
@Data
public class AnswerVO {

    private Long id;
    private Long questionId;
    private Long userId;
    private String content;
    private Integer helpfulCount;
    private Integer accepted;
    private Integer status;
    private LocalDateTime createTime;
    /** 作者信息 */
    private String authorNickname;
    private String authorAvatar;
    /** 当前用户状态 */
    private Boolean helpfuled;
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canAccept;
}