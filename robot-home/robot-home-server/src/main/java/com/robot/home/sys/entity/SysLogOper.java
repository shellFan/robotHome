package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理操作日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log_oper")
public class SysLogOper extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String module;
    private String action;
    private String method;
    /** 已脱敏的参数 */
    private String params;
    private String ip;
    private Integer status;
    private String errorMsg;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
