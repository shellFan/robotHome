package com.robot.home.commercial.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商业化审计日志(只增不改,无逻辑删除)
 */
@Data
@TableName("commercial_audit_log")
public class CommercialAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    /** 操作者类型: USER/ADMIN/SYSTEM */
    private String operatorType;
    private Long companyId;
    private String action;
    private String entityType;
    private Long entityId;
    /** 详情(JSON) */
    private String detail;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}