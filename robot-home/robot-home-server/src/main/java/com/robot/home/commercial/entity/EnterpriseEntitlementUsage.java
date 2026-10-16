package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权益使用记录
 */
@Data
@TableName("enterprise_entitlement_usage")
public class EnterpriseEntitlementUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long companyId;
    private Long entitlementId;
    private String entCode;
    /** 使用类型: CONSUME/RESERVE/RELEASE/RESET */
    private String usageType;
    private String referenceType;
    private Long referenceId;
    /** 额度变化(正消耗/负释放) */
    private Integer delta;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}