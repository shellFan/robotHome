package com.robot.home.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.robot.home.user.dto.UserProfileDTO;
import com.robot.home.user.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    User getByUsername(String username);

    User getByPhone(String phone);

    User getByOpenid(String openid);

    /**
     * 分页查询用户（后台）
     */
    IPage<User> pageList(Map<String, Object> params);

    /**
     * 用户注册（手机号 / 用户名）
     */
    User register(String username, String phone, String password, String nickname, String source);

    /**
     * 微信登录：存在则登录，否则自动注册
     */
    User wxLogin(String openid, String nickname, String avatar);

    void updateLastLogin(Long userId, String ip);

    /**
     * 更新个人资料
     */
    void updateProfile(Long userId, UserProfileDTO dto);
}
