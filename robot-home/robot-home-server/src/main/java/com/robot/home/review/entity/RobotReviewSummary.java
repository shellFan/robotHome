package com.robot.home.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.TimeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 评价汇总（每个robot一条记录）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_review_summary")
public class RobotReviewSummary extends TimeEntity {

    private Long robotId;
    private Integer reviewCount;
    private BigDecimal overallAvg;
    private BigDecimal qualityAvg;
    private BigDecimal serviceAvg;
    private BigDecimal costAvg;
    private Integer score5Count;
    private Integer score4Count;
    private Integer score3Count;
    private Integer score2Count;
    private Integer score1Count;
}