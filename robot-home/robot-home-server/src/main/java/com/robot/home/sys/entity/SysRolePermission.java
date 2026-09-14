package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 角色-权限
 */
@Getter
@Setter
@TableName("sys_role_permission")
public class SysRolePermission extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long roleId;
    private Long permissionId;

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