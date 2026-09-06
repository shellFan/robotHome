package com.robot.home.auth.controller;

import com.robot.home.auth.service.AuthService;
import com.robot.home.common.Result;
import com.robot.home.security.UserContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 前台认证接口（PC / 小程序统一）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String username,
                                             @RequestParam String password,
                                             HttpServletRequest request) {
        return Result.success(authService.loginByPassword(username, password, request.getRemoteAddr()));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestParam(required = false) String username,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam String password,
                                                @RequestParam(required = false) String nickname,
                                                HttpServletRequest request) {
        return Result.success(authService.register(username, phone, password, nickname, request.getRemoteAddr()));
    }

    @PostMapping("/sms-login")
    public Result<Map<String, Object>> smsLogin(@RequestParam String phone,
                                                @RequestParam String code,
                                                HttpServletRequest request) {
        return Result.success(authService.loginBySms(phone, code, request.getRemoteAddr()));
    }

    @PostMapping("/send-sms-code")
    public Result<Map<String, Object>> sendSmsCode(@RequestParam String phone,
                                                   @RequestParam(defaultValue = "login") String type) {
        return Result.success(authService.sendSmsCode(phone, type));
    }

    @GetMapping("/captcha")
    public Result<Map<String, Object>> captcha() {
        return Result.success(authService.captcha());
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestParam String refreshToken) {
        return Result.success(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        authService.logout(token);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<Object> me() {
        Long userId = UserContext.getUserId();
        return Result.success(authService.me(userId));
    }

    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestParam(required = false) String code,
                                               @RequestParam(required = false) String openid,
                                               @RequestParam(required = false) String nickname,
                                               @RequestParam(required = false) String avatar,
                                               HttpServletRequest request) {
        // 优先使用微信授权码换取openid（安全方式）
        // 若提供code，由后端调用微信API验证；若仅提供openid，仅dev模式允许
        return Result.success(authService.wxLogin(code, openid, nickname, avatar, request.getRemoteAddr()));
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return request.getParameter("token");
    }
}
