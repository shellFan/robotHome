package com.robot.home.qa.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题视图
 */
@Data
public class QuestionVO {

    private Long id;
    private Long userId;
    private Long robotId;
    private String robotName;
    private String title;
    private String content;
    private Integer status;
    private Integer answerCount;
    private Integer followCount;
    private Integer viewCount;
    private Integer hasAccepted;
    private LocalDateTime createTime;
    /** 作者信息 */
    private String authorNickname;
    private String authorAvatar;
    /** 当前用户状态 */
    private Boolean followed;
    private Boolean canEdit;
    private Boolean canDelete;
}