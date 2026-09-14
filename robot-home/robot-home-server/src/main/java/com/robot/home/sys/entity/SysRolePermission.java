package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;

import java.util.Objects;

/**
 * 角色-权限
 */
@TableName("sys_role_permission")
public class SysRolePermission extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long roleId;
    private Long permissionId;

    // --- 继承字段覆盖（确保编译可见） ---
    @Override
    public Long getId() { return super.getId(); }
    @Override
    public void setId(Long id) { super.setId(id); }

    // --- 自身字段 getter/setter ---
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public Long getPermissionId() { return permissionId; }
    public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }

    @Override
    public String toString() {
        return "SysRolePermission{id=" + getId() + ", roleId=" + roleId + ", permissionId=" + permissionId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysRolePermission that = (SysRolePermission) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() { return Objects.hash(getId()); }
}