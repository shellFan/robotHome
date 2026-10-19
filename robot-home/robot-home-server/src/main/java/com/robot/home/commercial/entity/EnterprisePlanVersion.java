package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 套餐版本(权益快照)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("enterprise_plan_version")
public class EnterprisePlanVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long planId;
    private Integer version;
    /** 版本名称快照 */
    private String name;
    /** 版本价格(分) */
    private Long priceCents;
    private Integer durationDays;
    /** 权益配置JSON */
    private String entitlementConfig;
    /** 状态: ACTIVE/ARCHIVED */
    private String status;
    private LocalDateTime effectiveTime;
}