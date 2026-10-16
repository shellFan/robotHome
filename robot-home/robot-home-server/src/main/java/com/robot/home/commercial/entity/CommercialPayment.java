package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商业化支付记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("commercial_payment")
public class CommercialPayment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private String paymentChannel;
    /** 渠道侧支付单号 */
    private String channelPaymentNo;
    /** 金额(分) */
    private Long amountCents;
    /** 状态: INIT/SUCCESS/FAIL */
    private String status;
    /** 回调原始报文 */
    private String callbackRaw;
    /** 验签结果 */
    private String verifyResult;
    private LocalDateTime callbackTime;
}