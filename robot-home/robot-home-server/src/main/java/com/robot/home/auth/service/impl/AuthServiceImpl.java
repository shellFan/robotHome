package com.robot.home.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.robot.home.auth.service.AuthService;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.AuthenticationException;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.exception.ValidationException;
import com.robot.home.common.util.JwtUtils;
import com.robot.home.common.util.PasswordUtil;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.security.LoginUser;
import com.robot.home.user.entity.User;
import com.robot.home.user.service.UserService;
import com.robot.home.user.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserService userService;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisUtils redisUtils;

    @Value("${robot.sms-code-expire:300}")
    private long smsCodeExpire;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Value("${robot.sms-dev-mode:true}")
    private boolean smsDevMode;

    private UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 手机号脱敏
        if (StrUtil.isNotBlank(vo.getPhone()) && vo.getPhone().length() >= 11) {
            vo.setPhone(vo.getPhone().substring(0, 3) + "****" + vo.getPhone().substring(7));
        }
        return vo;
    }

    private Map<String, Object> buildTokenResult(User user) {
        String role = Constants.ROLE_USER;
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), role);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        // 登录成功写入 Redis（用于登出黑名单反向校验 / 会话管理）
        redisUtils.set(Constants.CACHE_TOKEN_PREFIX + user.getId(), token, jwtExpiration / 1000, TimeUnit.SECONDS);
        Map<String, Object> result = new HashMap<>(8);
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("expiresIn", jwtExpiration / 1000);
        result.put("user", toVO(user));
        return result;
    }

    @Override
    public Map<String, Object> loginByPassword(String username, String password, String ip) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new ValidationException("用户名和密码不能为空");
        }
        User user = userService.getByUsername(username);
        if (user == null) {
            user = userService.getByPhone(username);
        }
        if (user == null) {
            throw new AuthenticationException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new AuthenticationException("账号已被禁用");
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new AuthenticationException("密码错误");
        }
        userService.updateLastLogin(user.getId(), ip);
        return buildTokenResult(user);
    }

    @Override
    public Map<String, Object> loginBySms(String phone, String code, String ip) {
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            throw new ValidationException("手机号和验证码不能为空");
        }
        String cached = redisUtils.get(Constants.CACHE_SMS_CODE_PREFIX + phone);
        if (StrUtil.isBlank(cached)) {
            throw new AuthenticationException("验证码已过期，请重新获取");
        }
        if (!cached.equals(code)) {
            throw new AuthenticationException("验证码错误");
        }
        redisUtils.delete(Constants.CACHE_SMS_CODE_PREFIX + phone);
        User user = userService.getByPhone(phone);
        if (user == null) {
            user = userService.register(null, phone, IdUtil.fastSimpleUUID(), null, "miniapp");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new AuthenticationException("账号已被禁用");
        }
        userService.updateLastLogin(user.getId(), ip);
        return buildTokenResult(user);
    }

    @Override
    public Map<String, Object> register(String username, String phone, String password, String nickname, String ip) {
        if (StrUtil.isBlank(phone) && StrUtil.isBlank(username)) {
            throw new ValidationException("用户名或手机号至少填写一项");
        }
        if (StrUtil.isBlank(password)) {
            throw new ValidationException("密码不能为空");
        }
        User user = userService.register(username, phone, password, nickname, "pc");
        return buildTokenResult(user);
    }

    @Override
    public Map<String, Object> wxLogin(String openid, String nickname, String avatar, String ip) {
        if (StrUtil.isBlank(openid)) {
            throw new ValidationException("openid 不能为空");
        }
        User user = userService.wxLogin(openid, nickname, avatar);
        return buildTokenResult(user);
    }

    @Override
    public Map<String, Object> sendSmsCode(String phone, String type) {
        if (StrUtil.isBlank(phone) || phone.length() != 11) {
            throw new ValidationException("手机号格式不正确");
        }
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        redisUtils.set(Constants.CACHE_SMS_CODE_PREFIX + phone, code, smsCodeExpire, TimeUnit.SECONDS);
        Map<String, Object> result = new HashMap<>(4);
        result.put("phone", phone);
        result.put("expire", smsCodeExpire);
        // 无真实短信通道，dev 模式直接返回验证码以便联调
        if (smsDevMode) {
            result.put("devCode", code);
        }
        return result;
    }

    @Override
    public Map<String, Object> captcha() {
        String code = String.format("%04d", (int) (Math.random() * 10000));
        String token = IdUtil.fastSimpleUUID();
        redisUtils.set(Constants.CACHE_CAPTCHA_PREFIX + token, code, 300, TimeUnit.SECONDS);
        Map<String, Object> result = new HashMap<>(4);
        result.put("captchaToken", token);
        if (smsDevMode) {
            result.put("code", code);
        }
        return result;
    }

    @Override
    public Map<String, Object> refresh(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            throw new AuthenticationException("refreshToken 不能为空");
        }
        try {
            Long userId = jwtUtils.getUserId(refreshToken);
            String username = jwtUtils.getUsername(refreshToken);
            User user = userService.getById(userId);
            if (user == null) {
                throw new AuthenticationException("用户不存在");
            }
            return buildTokenResult(user);
        } catch (Exception e) {
            throw new AuthenticationException("refreshToken 无效或已过期");
        }
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        try {
            long remain = jwtUtils.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
            if (remain > 0) {
                redisUtils.set(Constants.CACHE_TOKEN_PREFIX + "black:" + token, "1", remain / 1000, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public UserVO me(Long userId) {
        if (userId == null) {
            throw new AuthenticationException("请先登录");
        }
        return toVO(userService.getById(userId));
    }
}
