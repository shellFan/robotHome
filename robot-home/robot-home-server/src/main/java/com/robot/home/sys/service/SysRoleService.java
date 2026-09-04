package com.robot.home.sys.service;

import com.robot.home.sys.entity.SysRole;

import java.util.List;

/**
 * 角色服务
 */
public interface SysRoleService {

    List<SysRole> listAll();

    SysRole detail(Long id);

    /**
     * 保存角色及菜单、权限关联
     */
    Long save(SysRole role, List<Long> menuIds, List<Long> permissionIds);

    void remove(Long id);
}
