package com.robot.home.robot.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相关问答预览VO（RobotDetail首页聚合用）
 */
@Data
public class RelatedQuestionVO {
    private Long id;
    private String title;
    private Integer answerCount;
    private Integer followCount;
    private LocalDateTime createTime;
}