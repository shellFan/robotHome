package com.robot.home.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 线索推荐
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lead_recommendation")
public class LeadRecommendation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long companyId;
    private Long procurementId;
    /** 推荐分值 */
    private Integer score;
    private String reason;
    /** 状态: PENDING/ACCEPTED/REJECTED/EXPIRED */
    private String status;
    private LocalDateTime recommendedTime;
}