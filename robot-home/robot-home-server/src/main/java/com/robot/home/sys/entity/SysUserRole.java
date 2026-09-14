package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;

import java.util.Objects;

/**
 * 管理员-角色
 */
@TableName("sys_user_role")
public class SysUserRole extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long roleId;

    // --- 继承字段覆盖（确保编译可见） ---
    @Override
    public Long getId() { return super.getId(); }
    @Override
    public void setId(Long id) { super.setId(id); }

    // --- 自身字段 getter/setter ---
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    @Override
    public String toString() {
        return "SysUserRole{id=" + getId() + ", userId=" + userId + ", roleId=" + roleId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysUserRole that = (SysUserRole) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() { return Objects.hash(getId()); }
}