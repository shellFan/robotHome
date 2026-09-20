package com.robot.home.trust.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 机器人变更记录（信任体系）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_change_record")
public class RobotChangeRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long robotId;
    /** 变更类型: UPDATE/CORRECTION/OFFICIAL */
    private String changeType;
    private String fieldName;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
    private String sourceType;
    private String sourceName;
    /** 是否已验证 */
    private Integer verified;
    /** 事件唯一键（用于去重） */
    private String eventKey;
    private LocalDateTime changeTime;
}