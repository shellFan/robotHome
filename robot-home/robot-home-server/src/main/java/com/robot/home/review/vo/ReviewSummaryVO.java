package com.robot.home.review.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 评价汇总视图
 */
@Data
public class ReviewSummaryVO {
    private Long robotId;
    private Integer reviewCount;
    private BigDecimal overallAvg;
    private BigDecimal qualityAvg;
    private BigDecimal serviceAvg;
    private BigDecimal costAvg;
    private Integer score1Count;
    private Integer score2Count;
    private Integer score3Count;
    private Integer score4Count;
    private Integer score5Count;
}