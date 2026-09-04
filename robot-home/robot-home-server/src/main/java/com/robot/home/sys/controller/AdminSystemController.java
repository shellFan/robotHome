package com.robot.home.sys.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.security.RequirePermission;
import com.robot.home.sys.entity.SysConfig;
import com.robot.home.sys.entity.SysDict;
import com.robot.home.sys.entity.SysLogError;
import com.robot.home.sys.entity.SysLogLogin;
import com.robot.home.sys.entity.SysLogOper;
import com.robot.home.sys.entity.SysMenu;
import com.robot.home.sys.entity.SysPermission;
import com.robot.home.sys.entity.SysRole;
import com.robot.home.sys.entity.SysUser;
import com.robot.home.sys.mapper.SysConfigMapper;
import com.robot.home.sys.mapper.SysDictMapper;
import com.robot.home.sys.mapper.SysLogErrorMapper;
import com.robot.home.sys.mapper.SysLogLoginMapper;
import com.robot.home.sys.mapper.SysLogOperMapper;
import com.robot.home.sys.mapper.SysPermissionMapper;
import com.robot.home.sys.service.SysMenuService;
import com.robot.home.sys.service.SysRoleService;
import com.robot.home.sys.service.SysUserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 后台系统管理：管理员 / 角色 / 菜单 / 权限 / 日志 / 字典 / 配置
 */
@RestController
@RequestMapping("/api/admin/system")
public class AdminSystemController {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysRoleService sysRoleService;
    @Resource
    private SysMenuService sysMenuService;
    @Resource
    private SysPermissionMapper permissionMapper;
    @Resource
    private SysLogLoginMapper loginLogMapper;
    @Resource
    private SysLogOperMapper operLogMapper;
    @Resource
    private SysLogErrorMapper errorLogMapper;
    @Resource
    private SysDictMapper dictMapper;
    @Resource
    private SysConfigMapper configMapper;

    // ---------------- 管理员 ----------------

    @GetMapping("/admins")
    @RequirePermission("system:admin")
    public Result<PageResult<SysUser>> admins(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(sysUserService.page(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/admins/{id}")
    @RequirePermission("system:admin")
    public Result<SysUser> adminDetail(@PathVariable Long id) {
        return Result.success(sysUserService.detail(id));
    }

    @PostMapping("/admins")
    @RequirePermission("system:admin")
    public Result<Long> saveAdmin(@RequestBody SysUser user) {
        return Result.success(sysUserService.save(user, user.getRoleIds()));
    }

    @DeleteMapping("/admins/{id}")
    @RequirePermission("system:admin")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        sysUserService.remove(id);
        return Result.success();
    }

    @PostMapping("/admins/{id}/status")
    @RequirePermission("system:admin")
    public Result<Void> adminStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysUserService.updateStatus(id, status);
        return Result.success();
    }

    @PostMapping("/admins/{id}/password")
    @RequirePermission("system:admin")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        sysUserService.resetPassword(id, password);
        return Result.success();
    }

    // ---------------- 角色 ----------------

    @GetMapping("/roles")
    @RequirePermission("system:role")
    public Result<List<SysRole>> roles() {
        return Result.success(sysRoleService.listAll());
    }

    @GetMapping("/roles/{id}")
    @RequirePermission("system:role")
    public Result<SysRole> roleDetail(@PathVariable Long id) {
        return Result.success(sysRoleService.detail(id));
    }

    @PostMapping("/roles")
    @RequirePermission("system:role")
    public Result<Long> saveRole(@RequestBody SysRole role) {
        return Result.success(sysRoleService.save(role, role.getMenuIds(), role.getPermissionIds()));
    }

    @DeleteMapping("/roles/{id}")
    @RequirePermission("system:role")
    public Result<Void> deleteRole(@PathVariable Long id) {
        sysRoleService.remove(id);
        return Result.success();
    }

    // ---------------- 菜单 ----------------

    @GetMapping("/menus")
    @RequirePermission("system:menu")
    public Result<List<SysMenu>> menus() {
        return Result.success(sysMenuService.listAll());
    }

    @GetMapping("/menus/tree")
    @RequirePermission("system:menu")
    public Result<List<SysMenu>> menuTree() {
        return Result.success(sysMenuService.tree());
    }

    @PostMapping("/menus")
    @RequirePermission("system:menu")
    public Result<Long> saveMenu(@RequestBody SysMenu menu) {
        return Result.success(sysMenuService.saveMenu(menu));
    }

    @DeleteMapping("/menus/{id}")
    @RequirePermission("system:menu")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        sysMenuService.remove(id);
        return Result.success();
    }

    // ---------------- 权限 ----------------

    @GetMapping("/permissions")
    @RequirePermission("system:permission")
    public Result<List<SysPermission>> permissions() {
        return Result.success(permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                .orderByAsc(SysPermission::getId)));
    }

    @PostMapping("/permissions")
    @RequirePermission("system:permission")
    public Result<Long> savePermission(@RequestBody SysPermission permission) {
        if (StrUtil.isBlank(permission.getPermissionCode()) || StrUtil.isBlank(permission.getPermissionName())) {
            throw new BusinessException("权限标识与名称不能为空");
        }
        if (permission.getId() == null) {
            permissionMapper.insert(permission);
        } else {
            permissionMapper.updateById(permission);
        }
        return Result.success(permission.getId());
    }

    @DeleteMapping("/permissions/{id}")
    @RequirePermission("system:permission")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionMapper.deleteById(id);
        return Result.success();
    }

    // ---------------- 日志 ----------------

    @GetMapping("/logs/login")
    @RequirePermission("system:log")
    public Result<PageResult<SysLogLogin>> loginLogs(@RequestParam(required = false) String username,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<SysLogLogin> page = new Page<>(pn, ps);
        IPage<SysLogLogin> result = loginLogMapper.selectPage(page, Wrappers.<SysLogLogin>lambdaQuery()
                .like(StrUtil.isNotBlank(username), SysLogLogin::getUsername, username)
                .orderByDesc(SysLogLogin::getCreateTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/logs/oper")
    @RequirePermission("system:log")
    public Result<PageResult<SysLogOper>> operLogs(@RequestParam(required = false) String module,
                                                   @RequestParam(required = false) String username,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<SysLogOper> page = new Page<>(pn, ps);
        IPage<SysLogOper> result = operLogMapper.selectPage(page, Wrappers.<SysLogOper>lambdaQuery()
                .eq(StrUtil.isNotBlank(module), SysLogOper::getModule, module)
                .like(StrUtil.isNotBlank(username), SysLogOper::getUsername, username)
                .orderByDesc(SysLogOper::getCreateTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/logs/error")
    @RequirePermission("system:log")
    public Result<PageResult<SysLogError>> errorLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<SysLogError> page = new Page<>(pn, ps);
        IPage<SysLogError> result = errorLogMapper.selectPage(page, Wrappers.<SysLogError>lambdaQuery()
                .orderByDesc(SysLogError::getCreateTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    // ---------------- 字典 ----------------

    @GetMapping("/dicts")
    @RequirePermission("system:dict")
    public Result<List<SysDict>> dicts(@RequestParam(required = false) String dictType) {
        return Result.success(dictMapper.selectList(Wrappers.<SysDict>lambdaQuery()
                .eq(StrUtil.isNotBlank(dictType), SysDict::getDictType, dictType)
                .orderByAsc(SysDict::getSort)));
    }

    @PostMapping("/dicts")
    @RequirePermission("system:dict")
    public Result<Long> saveDict(@RequestBody SysDict dict) {
        if (StrUtil.isBlank(dict.getDictType()) || StrUtil.isBlank(dict.getDictValue())) {
            throw new BusinessException("字典类型与字典值不能为空");
        }
        if (dict.getId() == null) {
            if (dict.getStatus() == null) {
                dict.setStatus(1);
            }
            dictMapper.insert(dict);
        } else {
            dictMapper.updateById(dict);
        }
        return Result.success(dict.getId());
    }

    @DeleteMapping("/dicts/{id}")
    @RequirePermission("system:dict")
    public Result<Void> deleteDict(@PathVariable Long id) {
        dictMapper.deleteById(id);
        return Result.success();
    }

    // ---------------- 配置 ----------------

    @GetMapping("/configs")
    @RequirePermission("system:config")
    public Result<List<SysConfig>> configs() {
        return Result.success(configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .orderByAsc(SysConfig::getId)));
    }

    @PostMapping("/configs")
    @RequirePermission("system:config")
    public Result<Long> saveConfig(@RequestBody SysConfig config) {
        if (StrUtil.isBlank(config.getConfigKey())) {
            throw new BusinessException("配置键不能为空");
        }
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        return Result.success(config.getId());
    }

    @DeleteMapping("/configs/{id}")
    @RequirePermission("system:config")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        configMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 站点配置（前台 SEO 等使用）
     */
    @GetMapping("/site-config")
    public Result<Map<String, String>> siteConfig() {
        List<SysConfig> list = configMapper.selectList(Wrappers.<SysConfig>lambdaQuery());
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (SysConfig c : list) {
            if (StrUtil.isNotBlank(c.getConfigKey())) {
                map.put(c.getConfigKey(), c.getConfigValue());
            }
        }
        return Result.success(map);
    }
}
