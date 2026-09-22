package com.robot.home.procurement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 采购跟进记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("procurement_follow_record")
public class ProcurementFollowRecord extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long procurementId;
    private Long operatorId;
    private String operatorName;
    private String action;
    private String content;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime nextFollowTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}