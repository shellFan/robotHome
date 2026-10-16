package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 企业商业套餐
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("enterprise_plan")
public class EnterprisePlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 套餐编码: FREE/BASIC/PRO/ENTERPRISE */
    private String code;
    private String name;
    private String description;
    /** 套餐等级(越大越高) */
    private Integer level;
    /** 展示价格(仅展示,实际以版本为准) */
    private BigDecimal displayPrice;
    /** 默认有效期天数 */
    private Integer durationDays;
    private Integer sort;
    /** 状态: DRAFT/ON_SHELF/OFF_SHELF */
    private String status;
    /** 当前版本号 */
    private Integer currentVersion;
    /** 可售: 0否 1是 */
    private Integer saleable;
}