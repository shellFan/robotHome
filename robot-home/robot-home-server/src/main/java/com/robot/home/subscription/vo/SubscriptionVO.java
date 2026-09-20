package com.robot.home.subscription.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅VO（回填目标信息）
 */
@Data
public class SubscriptionVO {

    private Long id;
    /** 订阅目标类型: ROBOT / BRAND / COMPANY */
    private String targetType;
    private Long targetId;
    /** 订阅事件类型，逗号分隔 */
    private String eventTypes;
    private Integer enabled;
    /** 目标名称 */
    private String targetName;
    /** 目标头像/封面 */
    private String targetAvatar;
    /** 目标URL */
    private String targetUrl;
    private LocalDateTime createTime;
}