package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;

import java.util.List;
import java.util.Objects;

/**
 * 角色
 */
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

    // --- 继承字段覆盖（确保编译可见） ---
    @Override
    public Long getId() { return super.getId(); }
    @Override
    public void setId(Long id) { super.setId(id); }

    // --- 自身字段 getter/setter ---
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public List<Long> getMenuIds() { return menuIds; }
    public void setMenuIds(List<Long> menuIds) { this.menuIds = menuIds; }

    public List<Long> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }

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