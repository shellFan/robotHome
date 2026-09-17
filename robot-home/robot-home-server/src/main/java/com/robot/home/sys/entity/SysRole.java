package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

/**
 * 角色
 */
@Getter
@Setter
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

    @Override
    public String toString() {
        return "SysRole{id=" + getId() + ", roleCode=" + roleCode + ", roleName=" + roleName +
               ", description=" + description + ", status=" + status + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysRole sysRole = (SysRole) o;
        return Objects.equals(getId(), sysRole.getId());
    }

    @Override
    public int hashCode() { return Objects.hash(getId()); }
}