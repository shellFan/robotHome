package com.robot.home.common.exception;

/**
 * 参数校验异常
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;

    public ValidationException(String message) {
        super(message);
        this.code = 400;
    }

    public ValidationException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
