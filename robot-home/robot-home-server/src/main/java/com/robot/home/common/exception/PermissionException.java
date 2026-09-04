package com.robot.home.common.exception;

/**
 * 权限异常（已登录但无权限访问）
 */
public class PermissionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;

    public PermissionException(String message) {
        super(message);
        this.code = 403;
    }

    public PermissionException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
