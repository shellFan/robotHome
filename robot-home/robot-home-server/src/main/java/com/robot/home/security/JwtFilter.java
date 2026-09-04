package com.robot.home.security;

import com.robot.home.common.Constants;
import com.robot.home.common.exception.AuthenticationException;
import com.robot.home.common.util.JwtUtils;
import com.robot.home.common.util.RedisUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器：校验 Token，设置登录上下文
 */
@Component
public class JwtFilter implements HandlerInterceptor {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisUtils redisUtils;

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            // 允许匿名访问（具体接口可通过 @RequiresLogin 等再次校验，这里仅尝试解析）
            return true;
        }
        try {
            // 黑名单校验（登出后失效）
            if (redisUtils.hasKey(Constants.CACHE_TOKEN_PREFIX + "black:" + token)) {
                throw new AuthenticationException("登录已失效，请重新登录");
            }
            Long userId = jwtUtils.getUserId(token);
            String username = jwtUtils.getUsername(token);
            String role = jwtUtils.getRole(token);
            String type = jwtUtils.getType(token);
            LoginUser user = new LoginUser()
                    .setUserId(userId)
                    .setUsername(username)
                    .setRole(role)
                    .setToken(token);
            user.setUserType("admin".equals(type) ? Constants.USER_TYPE_ADMIN : Constants.USER_TYPE_NORMAL);
            UserContext.setUser(user);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("无效的登录凭证");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length()).trim();
        }
        String param = request.getParameter("token");
        if (StringUtils.hasText(param)) {
            return param.trim();
        }
        return null;
    }
}
