package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商业化订单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("commercial_order")
public class CommercialOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Long companyId;
    private Long userId;
    private Long planId;
    private Long planVersionId;
    /** 套餐快照(JSON) */
    private String planSnapshot;
    /** 原价(分) */
    private Long originalPriceCents;
    /** 应付金额(分) */
    private Long payableAmountCents;
    private String currency;
    /** 状态: PENDING/PAID/CLOSED/REFUNDED */
    private String status;
    private String paymentChannel;
    private String paymentNo;
    private String idempotentKey;
    private LocalDateTime payTime;
    private LocalDateTime closeTime;
}