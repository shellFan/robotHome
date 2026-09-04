package com.robot.home.common.exception;

/**
 * 认证异常（未登录 / token 失效 / 凭证错误）
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;

    public AuthenticationException(String message) {
        super(message);
        this.code = 401;
    }

    public AuthenticationException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
