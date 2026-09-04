package com.robot.home.sys;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.util.PasswordUtil;
import com.robot.home.sys.entity.SysRole;
import com.robot.home.sys.entity.SysUser;
import com.robot.home.sys.entity.SysUserRole;
import com.robot.home.sys.mapper.SysRoleMapper;
import com.robot.home.sys.mapper.SysUserMapper;
import com.robot.home.sys.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 保证初始超级管理员存在：
 * 若 sys_user 中不存在初始化账号，则自动创建并绑定 ADMIN 角色
 */
@Slf4j
@Component
@Order(1)
public class AdminDataInitializer implements CommandLineRunner {

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Value("${admin.init-username:admin}")
    private String initUsername;

    @Value("${admin.init-password:}")
    private String initPassword;

    @Override
    public void run(String... args) {
        // 未设置密码时跳过初始化（生产环境必须通过 ADMIN_INIT_PASSWORD 环境变量设置）
        if (initPassword == null || initPassword.trim().isEmpty()) {
            log.info("未设置 admin.init-password，跳过管理员初始化（生产环境必须通过环境变量 ADMIN_INIT_PASSWORD 设置）");
            return;
        }
        try {
            SysUser exist = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUsername, initUsername).last("LIMIT 1"));
            if (exist != null) {
                log.info("后台管理员账号已存在：{}", initUsername);
                return;
            }
            SysUser admin = new SysUser();
            admin.setUsername(initUsername);
            admin.setPassword(PasswordUtil.hash(initPassword));
            admin.setNickname("超级管理员");
            admin.setStatus(1);
            sysUserMapper.insert(admin);

            SysRole adminRole = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getRoleCode, "ADMIN").last("LIMIT 1"));
            if (adminRole != null) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(admin.getId());
                ur.setRoleId(adminRole.getId());
                sysUserRoleMapper.insert(ur);
            }
            log.info("已初始化后台管理员账号：{}", initUsername);
        } catch (Exception e) {
            // 数据库未就绪时不应阻断启动，仅提示
            log.warn("初始化后台管理员失败（请确认数据库已执行 sql 脚本）：{}", e.getMessage());
        }
    }
}
