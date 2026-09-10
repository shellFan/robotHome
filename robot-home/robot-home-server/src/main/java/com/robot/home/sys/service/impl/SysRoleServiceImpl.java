package com.robot.home.sys.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.sys.entity.SysRole;
import com.robot.home.sys.entity.SysRoleMenu;
import com.robot.home.sys.entity.SysRolePermission;
import com.robot.home.sys.entity.SysUserRole;
import com.robot.home.sys.mapper.SysRoleMapper;
import com.robot.home.sys.mapper.SysRoleMenuMapper;
import com.robot.home.sys.mapper.SysRolePermissionMapper;
import com.robot.home.sys.mapper.SysUserRoleMapper;
import com.robot.home.sys.service.SysRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private static final Logger log = LoggerFactory.getLogger(SysRoleServiceImpl.class);

    @Resource
    private SysRoleMenuMapper roleMenuMapper;
    @Resource
    private SysRolePermissionMapper rolePermissionMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public List<SysRole> listAll() {
        List<SysRole> roles = list(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
        for (SysRole role : roles) {
            role.setMenuIds(roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                    .eq(SysRoleMenu::getRoleId, role.getId()))
                    .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList()));
            role.setPermissionIds(rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                    .eq(SysRolePermission::getRoleId, role.getId()))
                    .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList()));
        }
        return roles;
    }

    @Override
    public SysRole detail(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setMenuIds(roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                .eq(SysRoleMenu::getRoleId, id)).stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList()));
        role.setPermissionIds(rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id)).stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList()));
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SysRole role, List<Long> menuIds, List<Long> permissionIds) {
        if (StrUtil.isBlank(role.getRoleCode()) || StrUtil.isBlank(role.getRoleName())) {
            throw new BusinessException("角色编码与角色名称不能为空");
        }
        if (role.getId() == null) {
            SysRole exist = getOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, role.getRoleCode()), false);
            if (exist != null) {
                throw new BusinessException("角色编码已存在");
            }
            if (role.getStatus() == null) {
                role.setStatus(1);
            }
            save(role);
        } else {
            SysRole exist = getById(role.getId());
            if (exist == null) {
                throw new BusinessException("角色不存在");
            }
            SysRole update = new SysRole();
            update.setId(role.getId());
            update.setRoleName(role.getRoleName());
            update.setDescription(role.getDescription());
            update.setStatus(role.getStatus());
            updateById(update);
        }
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, role.getId()));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(role.getId());
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getRoleId, role.getId()));
        if (permissionIds != null) {
            for (Long permissionId : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            }
        }
        // 清空该角色下所有管理员的权限缓存（Redis故障时降级跳过）
        List<SysUserRole> urs = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, role.getId()));
        Set<Long> userIds = urs.stream().map(SysUserRole::getUserId).collect(Collectors.toSet());
        for (Long userId : userIds) {
            try {
                redisUtils.delete(Constants.CACHE_TOKEN_PREFIX + "perm:" + userId);
            } catch (Exception e) {
                log.warn("Redis权限缓存清空失败: userId={}, error={}", userId, e.getMessage());
            }
        }
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (id == null || id == 1L) {
            throw new BusinessException("超级管理员角色不可删除");
        }
        removeById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getRoleId, id));
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, id));
    }
}
