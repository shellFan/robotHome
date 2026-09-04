package com.robot.home.sys.service;

import com.robot.home.common.PageResult;
import com.robot.home.sys.entity.SysMenu;
import com.robot.home.sys.entity.SysUser;
import com.robot.home.sys.vo.AdminInfoVO;

import java.util.List;
import java.util.Map;

/**
 * 后台管理员与 RBAC
 */
public interface SysUserService {

    /**
     * 后台登录，返回 token 等
     */
    Map<String, Object> login(String username, String password, String ip, String userAgent);

    /**
     * 当前管理员信息（含菜单树与权限码）
     */
    AdminInfoVO info(Long userId);

    PageResult<SysUser> page(String keyword, Integer status, Integer pageNum, Integer pageSize);

    SysUser detail(Long id);

    /**
     * 新增 / 更新管理员，roleIds 为角色 id 列表
     */
    Long save(SysUser user, List<Long> roleIds);

    void remove(Long id);

    void updateStatus(Long id, Integer status);

    /**
     * 重置密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 用户的权限码集合
     */
    List<String> permissionCodes(Long userId);

    /**
     * 用户的菜单树
     */
    List<SysMenu> menuTree(Long userId);
}
