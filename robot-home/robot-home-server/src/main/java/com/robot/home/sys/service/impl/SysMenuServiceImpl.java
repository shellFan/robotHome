package com.robot.home.sys.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.sys.entity.SysMenu;
import com.robot.home.sys.mapper.SysMenuMapper;
import com.robot.home.sys.service.SysMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单服务实现
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> listAll() {
        List<SysMenu> list = list(Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSort));
        for (SysMenu m : list) {
            m.setChildren(null);
        }
        return list;
    }

    @Override
    public List<SysMenu> tree() {
        return buildTree(listAll());
    }

    @Override
    public SysMenu detail(Long id) {
        SysMenu menu = getById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        return menu;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMenu(SysMenu menu) {
        if (StrUtil.isBlank(menu.getMenuName())) {
            throw new BusinessException("菜单名称不能为空");
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getMenuType() == null) {
            menu.setMenuType(1);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getId() == null) {
            save(menu);
        } else {
            updateById(menu);
        }
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        // 同时删除子菜单，避免出现孤儿节点
        List<Long> ids = new ArrayList<>();
        collectChildren(id, ids);
        ids.add(id);
        removeByIds(ids);
    }

    private void collectChildren(Long parentId, List<Long> ids) {
        List<SysMenu> children = list(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, parentId));
        for (SysMenu c : children) {
            ids.add(c.getId());
            collectChildren(c.getId(), ids);
        }
    }

    private List<SysMenu> buildTree(List<SysMenu> all) {
        List<SysMenu> roots = new ArrayList<>();
        Map<Long, SysMenu> index = new HashMap<>();
        for (SysMenu m : all) {
            m.setChildren(new ArrayList<>());
            index.put(m.getId(), m);
        }
        for (SysMenu m : all) {
            if (m.getParentId() == null || m.getParentId() == 0) {
                roots.add(m);
            } else {
                SysMenu parent = index.get(m.getParentId());
                if (parent != null) {
                    parent.getChildren().add(m);
                } else {
                    roots.add(m);
                }
            }
        }
        return roots;
    }
}
