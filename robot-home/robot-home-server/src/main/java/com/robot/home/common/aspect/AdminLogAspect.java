package com.robot.home.common.aspect;

import cn.hutool.json.JSONUtil;
import com.robot.home.common.util.SensitiveUtils;
import com.robot.home.security.UserContext;
import com.robot.home.sys.entity.SysLogOper;
import com.robot.home.sys.mapper.SysLogOperMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 后台操作日志切面：记录操作人、模块、动作、参数（脱敏）、IP、结果
 * 敏感数据（密码 / token）不落库
 */
@Aspect
@Component
public class AdminLogAspect {

    @Resource
    private SysLogOperMapper logMapper;

    @Resource
    private HttpServletRequest request;

    @Around("execution(* com.robot.home..controller..*(..))")
    public Object record(ProceedingJoinPoint point) throws Throwable {
        String uri = request == null ? null : request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/admin/") || uri.startsWith("/api/admin/auth/")) {
            return point.proceed();
        }
        long start = System.currentTimeMillis();
        int status = 1;
        String errorMsg = null;
        try {
            return point.proceed();
        } catch (Throwable e) {
            status = 0;
            errorMsg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw e;
        } finally {
            try {
                SysLogOper log = new SysLogOper();
                com.robot.home.security.LoginUser user = UserContext.getUser();
                log.setUserId(user == null ? null : user.getUserId());
                log.setUsername(user == null ? null : user.getUsername());
                Method method = ((MethodSignature) point.getSignature()).getMethod();
                log.setModule(resolveModule(point));
                log.setAction(method.getName());
                log.setMethod(request == null ? null : (request.getMethod() + " " + uri));
                log.setParams(SensitiveUtils.maskParams(safeArgs(point.getArgs())));
                log.setIp(request == null ? null : request.getRemoteAddr());
                log.setStatus(status);
                log.setErrorMsg(errorMsg == null ? null : (errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg));
                log.setCreateTime(LocalDateTime.now());
                logMapper.insert(log);
            } catch (Exception ignored) {
                // 日志记录失败不影响业务
            }
        }
    }

    private String safeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg instanceof javax.servlet.http.HttpServletRequest
                    || arg instanceof javax.servlet.http.HttpServletResponse) {
                continue;
            }
            try {
                sb.append(JSONUtil.toJsonStr(arg)).append(" | ");
            } catch (Exception ignored) {
                sb.append(arg.getClass().getSimpleName()).append(" | ");
            }
        }
        return sb.toString();
    }

    private String resolveModule(ProceedingJoinPoint point) {
        String className = point.getTarget() == null ? "" : point.getTarget().getClass().getSimpleName();
        return className.replace("Controller", "");
    }
}
