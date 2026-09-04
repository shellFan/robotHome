package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 异常日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log_error")
public class SysLogError extends IdEntity {

    private static final long serialVersionUID = 1L;

    private String message;
    private String stackTrace;
    private String ip;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
