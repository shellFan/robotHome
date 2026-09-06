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

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
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

    @Value("${robot.sms-dev-mode:false}")
    private boolean smsDevMode;

    @Value("${robot.wx-dev-mode:false}")
    private boolean wxDevMode;

    @Value("${robot.wx-appid:}")
    private String wxAppId;

    @Value("${robot.wx-secret:}")
    private String wxSecret;

    @Value("${robot.login-max-retry:5}")
    private int loginMaxRetry;

    /** 同一 IP 每小时最大注册次数 */
    private static final int REGISTER_MAX_PER_HOUR = 5;

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
        // 登录限流：同一账号连续失败达到阈值后锁定 10 分钟
        String failKey = Constants.CACHE_LIMIT_PREFIX + "user-login:" + username;
        String failCount = redisUtils.get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= loginMaxRetry) {
            throw new AuthenticationException("登录失败次数过多，请 10 分钟后再试");
        }
        User user = userService.getByUsername(username);
        if (user == null) {
            user = userService.getByPhone(username);
        }
        if (user == null) {
            long count = failCount == null ? 1 : Long.parseLong(failCount) + 1;
            redisUtils.set(failKey, String.valueOf(count), 600, TimeUnit.SECONDS);
            throw new AuthenticationException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new AuthenticationException("账号已被禁用");
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            long count = failCount == null ? 1 : Long.parseLong(failCount) + 1;
            redisUtils.set(failKey, String.valueOf(count), 600, TimeUnit.SECONDS);
            throw new AuthenticationException("密码错误");
        }
        // 登录成功清除失败计数
        redisUtils.delete(failKey);
        userService.updateLastLogin(user.getId(), ip);
        return buildTokenResult(user);
    }

    @Override
    public Map<String, Object> loginBySms(String phone, String code, String ip) {
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            throw new ValidationException("手机号和验证码不能为空");
        }
        // 短信登录限流：同一手机号连续失败达到阈值后锁定 10 分钟
        String failKey = Constants.CACHE_LIMIT_PREFIX + "sms-login:" + phone;
        String failCount = redisUtils.get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= loginMaxRetry) {
            throw new AuthenticationException("登录失败次数过多，请 10 分钟后再试");
        }
        String cached = redisUtils.get(Constants.CACHE_SMS_CODE_PREFIX + phone);
        if (StrUtil.isBlank(cached)) {
            throw new AuthenticationException("验证码已过期，请重新获取");
        }
        if (!cached.equals(code)) {
            long count = failCount == null ? 1 : Long.parseLong(failCount) + 1;
            redisUtils.set(failKey, String.valueOf(count), 600, TimeUnit.SECONDS);
            throw new AuthenticationException("验证码错误");
        }
        redisUtils.delete(Constants.CACHE_SMS_CODE_PREFIX + phone);
        // 登录成功清除失败计数
        redisUtils.delete(failKey);
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
        // 注册限流：同一 IP 每小时最多注册 5 次，防止批量注册
        String regKey = Constants.CACHE_LIMIT_PREFIX + "register:" + ip;
        String regCount = redisUtils.get(regKey);
        if (regCount != null && Integer.parseInt(regCount) >= REGISTER_MAX_PER_HOUR) {
            throw new ValidationException("注册过于频繁，请稍后再试");
        }
        User user = userService.register(username, phone, password, nickname, "pc");
        // 注册成功后递增计数
        long count = regCount == null ? 1 : Long.parseLong(regCount) + 1;
        redisUtils.set(regKey, String.valueOf(count), 3600, TimeUnit.SECONDS);
        return buildTokenResult(user);
    }

    @Override
    public Map<String, Object> wxLogin(String code, String openid, String nickname, String avatar, String ip) {
        // 安全方式：通过微信授权码换取openid
        if (StrUtil.isNotBlank(code) && StrUtil.isNotBlank(wxAppId) && StrUtil.isNotBlank(wxSecret)) {
            openid = wxCode2Session(code);
        }
        // 兼容方式：dev模式下允许直接传入openid（仅开发调试）
        if (StrUtil.isBlank(openid)) {
            throw new ValidationException("openid 不能为空");
        }
        if (!wxDevMode && StrUtil.isBlank(code)) {
            throw new ValidationException("微信登录必须提供授权码");
        }
        User user = userService.wxLogin(openid, nickname, avatar);
        return buildTokenResult(user);
    }

    /**
     * 调用微信 code2Session 接口换取 openid
     * 文档：https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
     */
    private String wxCode2Session(String code) {
        try {
            String url = String.format(
                    "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    wxAppId, wxSecret, code);
            String resp = cn.hutool.http.HttpUtil.get(url, 5000);
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(resp);
            if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
                throw new AuthenticationException("微信登录失败: " + json.getStr("errmsg", "unknown"));
            }
            return json.getStr("openid");
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("微信登录服务异常，请稍后重试");
        }
    }

    @Override
    public Map<String, Object> sendSmsCode(String phone, String type) {
        if (StrUtil.isBlank(phone) || phone.length() != 11) {
            throw new ValidationException("手机号格式不正确");
        }
        // 短信发送限流：同一手机号 60 秒内只能发送一次
        String sendKey = Constants.CACHE_LIMIT_PREFIX + "sms-send:" + phone;
        if (redisUtils.hasKey(sendKey)) {
            throw new ValidationException("发送过于频繁，请稍后再试");
        }
        String code = generateSecureCode(6);
        redisUtils.set(Constants.CACHE_SMS_CODE_PREFIX + phone, code, smsCodeExpire, TimeUnit.SECONDS);
        // 设置 60 秒发送间隔
        redisUtils.set(sendKey, "1", 60, TimeUnit.SECONDS);
        Map<String, Object> result = new HashMap<>(4);
        result.put("phone", phone);
        result.put("expire", smsCodeExpire);
        // 安全：不再在API响应中返回验证码，即使dev模式也不返回
        // dev模式下验证码通过日志输出便于调试
        if (smsDevMode) {
            log.info("[DEV] SMS code for phone={}: {}", phone, code);
        }
        return result;
    }

    @Override
    public Map<String, Object> captcha() {
        String code = generateSecureCode(4);
        String token = IdUtil.fastSimpleUUID();
        redisUtils.set(Constants.CACHE_CAPTCHA_PREFIX + token, code, 300, TimeUnit.SECONDS);
        Map<String, Object> result = new HashMap<>(4);
        result.put("captchaToken", token);
        // 安全：不再在API响应中返回验证码
        if (smsDevMode) {
            log.info("[DEV] Captcha code for token={}: {}", token, code);
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
        } catch (Exception e) {
            log.warn("logout处理异常, token可能已失效: {}", e.getMessage());
        }
    }

    @Override
    public UserVO me(Long userId) {
        if (userId == null) {
            throw new AuthenticationException("请先登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new AuthenticationException("用户不存在或已被删除");
        }
        return toVO(user);
    }

    /** 使用 SecureRandom 生成指定长度的数字验证码 */
    private String generateSecureCode(int digits) {
        SecureRandom random = new SecureRandom();
        int bound = (int) Math.pow(10, digits);
        return String.format("%0" + digits + "d", random.nextInt(bound));
    }
}
