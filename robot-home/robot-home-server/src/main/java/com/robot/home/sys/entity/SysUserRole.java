package com.robot.home.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 管理员-角色
 */
@Getter
@Setter
@TableName("sys_user_role")
public class SysUserRole extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long roleId;

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