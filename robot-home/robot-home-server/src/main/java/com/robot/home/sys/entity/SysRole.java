package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;

    /** 菜单 id 列表（非数据库字段） */
    @TableField(exist = false)
    private List<Long> menuIds;

    /** 权限 id 列表（非数据库字段） */
    @TableField(exist = false)
    private List<Long> permissionIds;
}
