package com.robot.home.sys.controller;

import com.robot.home.auth.service.AuthService;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.sys.service.SysUserService;
import com.robot.home.sys.vo.AdminInfoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 后台认证
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String username,
                                             @RequestParam String password,
                                             HttpServletRequest request) {
        String ip = clientIp(request);
        return Result.success(sysUserService.login(username, password, ip, request.getHeader("User-Agent")));
    }

    @GetMapping("/info")
    public Result<AdminInfoVO> info() {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(sysUserService.info(userId));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7).trim());
        }
        return Result.success();
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
