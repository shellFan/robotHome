package com.robot.home.sys.service;

import com.robot.home.sys.entity.SysMenu;

import java.util.List;

/**
 * 菜单服务
 */
public interface SysMenuService {

    /**
     * 全部菜单（平铺）
     */
    List<SysMenu> listAll();

    /**
     * 菜单树
     */
    List<SysMenu> tree();

    SysMenu detail(Long id);

    Long saveMenu(SysMenu menu);

    void remove(Long id);
}
