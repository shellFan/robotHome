package com.robot.home.security;

import com.robot.home.common.Constants;
import com.robot.home.common.exception.AuthenticationException;
import com.robot.home.common.exception.PermissionException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 后台管理接口拦截器：要求 ADMIN 角色
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 浏览器跨域预检不带 Token，必须放行，否则登录后的 GET 会报 Network Error
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        LoginUser user = UserContext.getUser();
        if (user == null) {
            throw new AuthenticationException("请先登录");
        }
        // 只有后台管理员签发的 token（type=admin）才允许访问管理接口
        if (!Integer.valueOf(Constants.USER_TYPE_ADMIN).equals(user.getUserType())) {
            throw new PermissionException("无权限访问管理后台");
        }
        return true;
    }
}
