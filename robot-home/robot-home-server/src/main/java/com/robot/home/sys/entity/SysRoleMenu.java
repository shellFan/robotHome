package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;

import java.util.Objects;

/**
 * 角色-菜单
 */
@TableName("sys_role_menu")
public class SysRoleMenu extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long roleId;
    private Long menuId;

    // --- 继承字段覆盖（确保编译可见） ---
    @Override
    public Long getId() { return super.getId(); }
    @Override
    public void setId(Long id) { super.setId(id); }

    // --- 自身字段 getter/setter ---
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }

    @Override
    public String toString() {
        return "SysRoleMenu{id=" + getId() + ", roleId=" + roleId + ", menuId=" + menuId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysRoleMenu that = (SysRoleMenu) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() { return Objects.hash(getId()); }
}