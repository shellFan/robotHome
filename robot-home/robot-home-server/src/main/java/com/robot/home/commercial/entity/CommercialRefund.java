package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商业化退款记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("commercial_refund")
public class CommercialRefund extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String refundNo;
    /** 金额(分) */
    private Long amountCents;
    /** 状态: APPLYING/SUCCESS/FAIL */
    private String status;
    private String reason;
    private Long operatorId;
    private LocalDateTime confirmTime;
}