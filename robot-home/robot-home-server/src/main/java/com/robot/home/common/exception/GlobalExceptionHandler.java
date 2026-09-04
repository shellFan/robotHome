package com.robot.home.common.exception;

import com.robot.home.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理：避免在每个 Controller 中散落的 try/catch
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode() == 0 ? 500 : e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public Result<Void> handleValidation(ValidationException e, HttpServletRequest request) {
        log.warn("参数校验异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode() == 0 ? 400 : e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuth(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode() == 0 ? 401 : e.getCode(), e.getMessage());
    }

    @ExceptionHandler(PermissionException.class)
    public Result<Void> handlePermission(PermissionException e, HttpServletRequest request) {
        log.warn("权限异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode() == 0 ? 403 : e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.error(400, "参数校验失败 - " + msg);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError) {
                        return ((FieldError) err).getField() + ":" + err.getDefaultMessage();
                    }
                    return err.getDefaultMessage();
                }).collect(Collectors.joining("; "));
        return Result.error(400, "参数绑定失败 - " + msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少必填参数: " + e.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        // 注意：不得记录密码、token 等敏感信息
        log.error("系统异常 [{}]", request.getRequestURI(), e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
