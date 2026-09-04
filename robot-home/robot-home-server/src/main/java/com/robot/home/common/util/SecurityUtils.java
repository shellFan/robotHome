package com.robot.home.common.util;

import com.robot.home.common.exception.AuthenticationException;
import com.robot.home.security.UserContext;

/**
 * 登录态工具：统一取值与校验，避免在 Controller 中散落判空
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 当前登录用户 id，未登录返回 null
     */
    public static Long currentUserId() {
        return UserContext.getUserId();
    }

    /**
     * 当前登录用户 id，未登录抛异常（返回码 401）
     */
    public static Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new AuthenticationException("请先登录");
        }
        return userId;
    }

    public static boolean isLogin() {
        return UserContext.getUserId() != null;
    }
}
