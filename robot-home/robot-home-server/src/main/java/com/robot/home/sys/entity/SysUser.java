package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 后台管理员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    /** 序列化时不得输出密码 */
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    /** 0禁用 1正常 */
    private Integer status;

    /** 角色 id 列表（非数据库字段） */
    @TableField(exist = false)
    private List<Long> roleIds;

    /** 角色编码列表（非数据库字段，用于 JWT） */
    @TableField(exist = false)
    private List<String> roleCodes;
}
