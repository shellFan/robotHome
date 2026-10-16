package com.robot.home.advertising.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 广告位
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("advertising_slot")
public class AdvertisingSlot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String slotCode;
    private String name;
    /** 页面位置标识 */
    private String pageLocation;
    /** 尺寸提示 */
    private String sizeHint;
    /** 允许的素材类型(JSON数组) */
    private String allowedTypes;
    /** 投放规则(JSON) */
    private String rules;
    /** 状态: ACTIVE/INACTIVE */
    private String status;
    private Integer sort;
}