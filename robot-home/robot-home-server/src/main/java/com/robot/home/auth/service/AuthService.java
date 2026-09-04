package com.robot.home.auth.service;

import com.robot.home.user.vo.UserVO;

import java.util.Map;

public interface AuthService {

    /**
     * 密码登录
     */
    Map<String, Object> loginByPassword(String username, String password, String ip);

    /**
     * 手机号 + 验证码登录（不存在则自动注册）
     */
    Map<String, Object> loginBySms(String phone, String code, String ip);

    /**
     * 注册
     */
    Map<String, Object> register(String username, String phone, String password, String nickname, String ip);

    /**
     * 微信登录（小程序）
     */
    Map<String, Object> wxLogin(String openid, String nickname, String avatar, String ip);

    /**
     * 发送短信验证码（dev 模式直接返回验证码）
     */
    Map<String, Object> sendSmsCode(String phone, String type);

    /**
     * 获取图形验证码（dev 模式直接返回）
     */
    Map<String, Object> captcha();

    /**
     * 刷新 Token
     */
    Map<String, Object> refresh(String refreshToken);

    /**
     * 登出（Token 黑名单）
     */
    void logout(String token);

    /**
     * 当前登录用户信息
     */
    UserVO me(Long userId);
}
