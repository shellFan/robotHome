package com.robot.home.robot.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相关讨论预览VO（RobotDetail首页聚合用）
 */
@Data
public class RelatedPostVO {
    private Long id;
    private String title;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
}