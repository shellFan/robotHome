package com.robot.home.common.exception;

/**
 * 业务异常：用于业务逻辑校验失败（如参数非法、业务规则不满足等）
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
