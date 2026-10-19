package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 企业权益实例
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("enterprise_entitlement")
public class EnterpriseEntitlement extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long companyId;
    private Long subscriptionId;
    /** 权益编码 */
    private String entCode;
    /** 权益配置JSON */
    private String entConfig;
    /** 总额度(0=无限) */
    private Integer quotaTotal;
    /** 已用额度 */
    private Integer quotaUsed;
    /** 额度周期: MONTHLY/TOTAL */
    private String periodType;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    /** 状态: ACTIVE/EXPIRED/REVOKED */
    private String status;
    private LocalDateTime expireTime;
}