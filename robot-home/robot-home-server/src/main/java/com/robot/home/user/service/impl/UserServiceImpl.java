package com.robot.home.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PasswordUtil;
import com.robot.home.user.dto.UserProfileDTO;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import com.robot.home.user.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username), false);
    }

    @Override
    public User getByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        return getOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone), false);
    }

    @Override
    public User getByOpenid(String openid) {
        if (StrUtil.isBlank(openid)) {
            return null;
        }
        return getOne(Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid), false);
    }

    @Override
    public IPage<User> pageList(Map<String, Object> params) {
        int pageNum = params.get("pageNum") == null ? 1 : Integer.parseInt(params.get("pageNum").toString());
        int pageSize = params.get("pageSize") == null ? 20 : Integer.parseInt(params.get("pageSize").toString());
        Page<User> page = new Page<>(pageNum, pageSize);
        String keyword = (String) params.get("keyword");
        Integer status = params.get("status") == null ? null : Integer.parseInt(params.get("status").toString());
        return page(page, Wrappers.<User>lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), User::getUsername, keyword)
                .like(StrUtil.isNotBlank(keyword), User::getNickname, keyword)
                .like(StrUtil.isNotBlank(keyword), User::getPhone, keyword)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getId));
    }

    @Override
    public User register(String username, String phone, String password, String nickname, String source) {
        if (StrUtil.isNotBlank(username) && getByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (StrUtil.isNotBlank(phone) && getByPhone(phone) != null) {
            throw new BusinessException("手机号已注册");
        }
        User user = new User();
        user.setUsername(StrUtil.isNotBlank(username) ? username : phone);
        user.setPhone(phone);
        user.setPassword(PasswordUtil.hash(password));
        user.setNickname(StrUtil.isNotBlank(nickname) ? nickname : (StrUtil.isNotBlank(phone) ? maskPhone(phone) : username));
        user.setStatus(1);
        user.setUserType(1);
        user.setSource(source);
        user.setLastLoginTime(LocalDateTime.now());
        save(user);
        return user;
    }

    @Override
    public User wxLogin(String openid, String nickname, String avatar) {
        User user = getByOpenid(openid);
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(nickname);
            user.setAvatar(avatar);
            user.setUsername("wx_" + openid);
            user.setStatus(1);
            user.setUserType(1);
            user.setSource("miniapp");
            user.setLastLoginTime(LocalDateTime.now());
            save(user);
        } else {
            user.setLastLoginTime(LocalDateTime.now());
            if (StrUtil.isNotBlank(nickname)) {
                user.setNickname(nickname);
            }
            if (StrUtil.isNotBlank(avatar)) {
                user.setAvatar(avatar);
            }
            updateById(user);
        }
        return user;
    }

    @Override
    public void updateLastLogin(Long userId, String ip) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        updateById(user);
    }

    @Override
    public void updateProfile(Long userId, UserProfileDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        User update = new User();
        update.setId(userId);
        if (StrUtil.isNotBlank(dto.getNickname())) {
            update.setNickname(StrUtil.trim(dto.getNickname()));
        }
        if (dto.getAvatar() != null) {
            update.setAvatar(dto.getAvatar());
        }
        if (dto.getIntro() != null) {
            update.setIntro(dto.getIntro());
        }
        if (dto.getGender() != null) {
            update.setGender(dto.getGender());
        }
        if (dto.getProvince() != null) {
            update.setProvince(dto.getProvince());
        }
        if (dto.getCity() != null) {
            update.setCity(dto.getCity());
        }
        if (dto.getEmail() != null) {
            update.setEmail(dto.getEmail());
        }
        updateById(update);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
