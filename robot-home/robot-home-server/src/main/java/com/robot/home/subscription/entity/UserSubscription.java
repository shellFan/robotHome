package com.robot.home.subscription.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户订阅
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_subscription")
public class UserSubscription extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    /** 订阅目标类型: robot/brand/company/topic/user */
    private String targetType;
    private Long targetId;
    /** 订阅事件类型JSON数组 */
    private String eventTypes;
    private Integer enabled;
}