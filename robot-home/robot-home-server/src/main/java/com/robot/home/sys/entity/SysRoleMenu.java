package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 角色-菜单
 */
@Getter
@Setter
@TableName("sys_role_menu")
public class SysRoleMenu extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long roleId;
    private Long menuId;

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