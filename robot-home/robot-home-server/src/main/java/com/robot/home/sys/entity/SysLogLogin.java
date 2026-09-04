package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log_login")
public class SysLogLogin extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    /** 1成功 0失败 */
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
