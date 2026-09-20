package com.robot.home.reputation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 信誉事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reputation_event")
public class ReputationEvent extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String eventType;
    private String eventKey;
    private Integer scoreDelta;
    private String referenceType;
    private Long referenceId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}