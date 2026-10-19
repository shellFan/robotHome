package com.robot.home.advertising.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 广告投放计划
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("advertising_campaign")
public class AdvertisingCampaign extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long companyId;
    private Long slotId;
    private String name;
    /** 状态: DRAFT/PENDING_REVIEW/APPROVED/REJECTED/ONLINE/OFFLINE/ENDED */
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 总预算(分) */
    private Long budgetCents;
    private Integer dailyLimit;
    private Integer totalLimit;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String reviewNote;
}