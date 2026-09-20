package com.robot.home.reputation.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * P0-7: 信誉事件视图对象
 */
@Data
public class ReputationEventVO {

    private Long id;

    private Long userId;

    /** 事件类型 */
    private String eventType;

    /** 事件类型中文 */
    private String eventTypeLabel;

    /** 分数变动（正/负） */
    private Integer scoreDelta;

    /** 引用类型: CORRECTION/ANSWER/REVIEW/POST/QUESTION */
    private String referenceType;

    /** 引用ID */
    private Long referenceId;

    /** 创建时间 */
    private LocalDateTime createTime;
}