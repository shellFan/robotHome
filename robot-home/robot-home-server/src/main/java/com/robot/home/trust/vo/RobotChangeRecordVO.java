package com.robot.home.trust.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 变更记录 VO（公开，仅展示白名单字段）
 */
@Data
public class RobotChangeRecordVO {

    private Long id;
    private Long robotId;
    /** 变更类型: PRICE/PARAMETER/VERSION/STATUS/BASIC_INFO */
    private String changeType;
    private String fieldName;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
    private String sourceType;
    private String sourceName;
    /** 是否已验证 */
    private Integer verified;
    private LocalDateTime changeTime;
}