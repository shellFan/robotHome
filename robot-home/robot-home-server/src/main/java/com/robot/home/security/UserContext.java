package com.robot.home.security;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT = new ThreadLocal<>();

    public static void setUser(LoginUser user) {
        CURRENT.set(user);
    }

    public static LoginUser getUser() {
        return CURRENT.get();
    }

    public static Long getUserId() {
        LoginUser user = CURRENT.get();
        return user == null ? null : user.getUserId();
    }

    public static String getRole() {
        LoginUser user = CURRENT.get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
