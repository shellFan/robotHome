package com.robot.home.sys.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.AuthenticationException;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.JwtUtils;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.PasswordUtil;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.sys.entity.SysLogLogin;
import com.robot.home.sys.entity.SysMenu;
import com.robot.home.sys.entity.SysPermission;
import com.robot.home.sys.entity.SysRole;
import com.robot.home.sys.entity.SysRoleMenu;
import com.robot.home.sys.entity.SysRolePermission;
import com.robot.home.sys.entity.SysUser;
import com.robot.home.sys.entity.SysUserRole;
import com.robot.home.sys.mapper.SysLogLoginMapper;
import com.robot.home.sys.mapper.SysMenuMapper;
import com.robot.home.sys.mapper.SysPermissionMapper;
import com.robot.home.sys.mapper.SysRoleMapper;
import com.robot.home.sys.mapper.SysRoleMenuMapper;
import com.robot.home.sys.mapper.SysRolePermissionMapper;
import com.robot.home.sys.mapper.SysUserMapper;
import com.robot.home.sys.mapper.SysUserRoleMapper;
import com.robot.home.sys.service.SysUserService;
import com.robot.home.sys.vo.AdminInfoVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 后台管理员服务实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysRoleMenuMapper roleMenuMapper;
    @Resource
    private SysRolePermissionMapper rolePermissionMapper;
    @Resource
    private SysMenuMapper menuMapper;
    @Resource
    private SysPermissionMapper permissionMapper;
    @Resource
    private SysLogLoginMapper loginLogMapper;
    @Resource
    private JwtUtils jwtUtils;
    @Resource
    private RedisUtils redisUtils;

    @Value("${robot.login-max-retry:5}")
    private int loginMaxRetry;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(String username, String password, String ip, String userAgent) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new AuthenticationException("用户名和密码不能为空");
        }
        // 登录限流：同一账号连续失败达到阈值后锁定 10 分钟
        String failKey = Constants.CACHE_LIMIT_PREFIX + "login:" + username;
        String failCount = redisUtils.get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= loginMaxRetry) {
            recordLoginLog(null, username, ip, userAgent, 0);
            throw new AuthenticationException("登录失败次数过多，请 10 分钟后再试");
        }

        SysUser user = getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username), false);
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            long count = failCount == null ? 1 : Long.parseLong(failCount) + 1;
            redisUtils.set(failKey, String.valueOf(count), 600, TimeUnit.SECONDS);
            recordLoginLog(null, username, ip, userAgent, 0);
            throw new AuthenticationException("用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            recordLoginLog(user.getId(), username, ip, userAgent, 0);
            throw new AuthenticationException("账号已被禁用");
        }
        redisUtils.delete(failKey);

        List<SysRole> roles = rolesOf(user.getId());
        String roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.joining(","));
        if (StrUtil.isBlank(roleCodes)) {
            roleCodes = Constants.ROLE_USER;
        }
        String token = jwtUtils.generateAdminToken(user.getId(), user.getUsername(), roleCodes);

        // 权限码缓存：供权限切面校验
        List<String> permissions = permissionCodes(user.getId());
        redisUtils.set(Constants.CACHE_TOKEN_PREFIX + "perm:" + user.getId(),
                String.join(",", permissions), jwtExpiration / 1000, TimeUnit.SECONDS);

        recordLoginLog(user.getId(), username, ip, userAgent, 1);

        Map<String, Object> result = new HashMap<>(8);
        result.put("token", token);
        result.put("expiresIn", jwtExpiration / 1000);
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        return result;
    }

    @Override
    public AdminInfoVO info(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new AuthenticationException("管理员不存在");
        }
        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(rolesOf(userId).stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        vo.setPermissions(permissionCodes(userId));
        vo.setMenus(menuTree(userId));
        return vo;
    }

    @Override
    public PageResult<SysUser> page(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<SysUser> page = new Page<>(pn, ps);
        IPage<SysUser> result = page(page, Wrappers.<SysUser>lambdaQuery()
                .likeRight(StrUtil.isNotBlank(keyword), SysUser::getUsername, keyword)
                .likeRight(StrUtil.isNotBlank(keyword), SysUser::getNickname, keyword)
                .eq(status != null, SysUser::getStatus, status)
                .orderByAsc(SysUser::getId));
        // 不返回密码
        for (SysUser u : result.getRecords()) {
            u.setPassword(null);
            u.setRoleIds(roleIdsOf(u.getId()));
        }
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public SysUser detail(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("管理员不存在");
        }
        user.setPassword(null);
        user.setRoleIds(roleIdsOf(id));
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SysUser user, List<Long> roleIds) {
        if (StrUtil.isBlank(user.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (user.getId() == null) {
            if (StrUtil.isBlank(user.getPassword())) {
                throw new BusinessException("密码不能为空");
            }
            SysUser exist = getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, user.getUsername()), false);
            if (exist != null) {
                throw new BusinessException("用户名已存在");
            }
            user.setPassword(PasswordUtil.hash(user.getPassword()));
            if (user.getStatus() == null) {
                user.setStatus(1);
            }
            save(user);
        } else {
            SysUser exist = getById(user.getId());
            if (exist == null) {
                throw new BusinessException("管理员不存在");
            }
            SysUser update = new SysUser();
            update.setId(user.getId());
            update.setNickname(user.getNickname());
            update.setAvatar(user.getAvatar());
            update.setPhone(user.getPhone());
            update.setEmail(user.getEmail());
            update.setStatus(user.getStatus());
            if (StrUtil.isNotBlank(user.getPassword())) {
                update.setPassword(PasswordUtil.hash(user.getPassword()));
            }
            updateById(update);
        }
        // 重新绑定角色
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, user.getId()));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                if (roleId == null) {
                    continue;
                }
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
        refreshPermissionCache(user.getId());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (id == null || id == 1L) {
            throw new BusinessException("初始超级管理员不可删除");
        }
        removeById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        redisUtils.delete(Constants.CACHE_TOKEN_PREFIX + "perm:" + id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (id == null || id == 1L) {
            throw new BusinessException("初始超级管理员不可禁用");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        if (StrUtil.isBlank(newPassword) || newPassword.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(PasswordUtil.hash(newPassword));
        updateById(update);
    }

    @Override
    public List<String> permissionCodes(Long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysRolePermission> rps = rolePermissionMapper.selectList(
                Wrappers.<SysRolePermission>lambdaQuery().in(SysRolePermission::getRoleId, roleIds));
        List<Long> permissionIds = rps.stream().map(SysRolePermission::getPermissionId).distinct().collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
        // 去重，避免角色间权限重复
        Set<String> codes = new LinkedHashSet<>();
        for (SysPermission p : permissions) {
            if (p != null && StrUtil.isNotBlank(p.getPermissionCode())) {
                codes.add(p.getPermissionCode());
            }
        }
        return new ArrayList<>(codes);
    }

    @Override
    public List<SysMenu> menuTree(Long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        boolean isAdmin = rolesOf(userId).stream().anyMatch(r -> Constants.ROLE_ADMIN.equals(r.getRoleCode()));
        List<SysMenu> all = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSort));
        if (!isAdmin) {
            List<SysRoleMenu> rms = roleMenuMapper.selectList(
                    Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds));
            Set<Long> menuIds = rms.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
            all = all.stream().filter(m -> menuIds.contains(m.getId())).collect(Collectors.toList());
        }
        return buildTree(all);
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

    private List<Long> roleIdsOf(Long userId) {
        List<SysUserRole> urs = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        return urs.stream().map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());
    }

    private List<SysRole> rolesOf(Long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    private void refreshPermissionCache(Long userId) {
        List<String> permissions = permissionCodes(userId);
        redisUtils.set(Constants.CACHE_TOKEN_PREFIX + "perm:" + userId,
                String.join(",", permissions), jwtExpiration / 1000, TimeUnit.SECONDS);
    }

    private void recordLoginLog(Long userId, String username, String ip, String userAgent, int status) {
        SysLogLogin log = new SysLogLogin();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(ip);
        log.setUserAgent(userAgent == null ? null : (userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent));
        log.setStatus(status);
        log.setCreateTime(java.time.LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}
