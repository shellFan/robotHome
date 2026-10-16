package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 企业订阅
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("enterprise_subscription")
public class EnterpriseSubscription extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long companyId;
    private Long planId;
    private Long planVersionId;
    /** 套餐名称快照 */
    private String planName;
    /** 状态: FREE/ACTIVE/EXPIRING/EXPIRED/CANCELLED/SUSPENDED */
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 自动续费标记(仅标记,不自动扣款) */
    private Integer autoRenew;
    private Long sourceOrderId;
    private LocalDateTime cancelTime;
    private String suspendReason;
}