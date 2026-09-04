package com.robot.home.security;

import com.robot.home.common.Constants;
import com.robot.home.common.exception.PermissionException;
import com.robot.home.common.util.RedisUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 后台接口权限校验切面（RBAC）
 * 权限集合缓存在 Redis，角色或菜单变更后清理 key 即可生效
 */
@Aspect
@Component
public class PermissionAspect {

    @Resource
    private RedisUtils redisUtils;

    @Around("execution(* com.robot.home..controller..*(..))")
    public Object check(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = resolveAnnotation(point, method);
        if (annotation == null) {
            return point.proceed();
        }
        LoginUser user = UserContext.getUser();
        if (user == null) {
            throw new PermissionException("请先登录");
        }
        if (hasRole(user, Constants.ROLE_ADMIN)) {
            return point.proceed();
        }
        String required = annotation.value();
        Set<String> permissions = permissionsOf(user.getUserId());
        if (!permissions.contains(required)) {
            throw new PermissionException("无操作权限：" + required);
        }
        return point.proceed();
    }

    /**
     * 解析方法上的权限注解：优先取接口方法，再回退到目标类方法（兼容 CGLIB 代理）
     */
    private RequirePermission resolveAnnotation(ProceedingJoinPoint point, Method method) {
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation != null) {
            return annotation;
        }
        Object target = point.getTarget();
        if (target == null) {
            return null;
        }
        try {
            Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            return targetMethod.getAnnotation(RequirePermission.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private boolean hasRole(LoginUser user, String roleCode) {
        if (user.getRole() == null) {
            return false;
        }
        for (String r : user.getRole().split(",")) {
            if (roleCode.equalsIgnoreCase(r.trim())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> permissionsOf(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        String cached = redisUtils.get(Constants.CACHE_TOKEN_PREFIX + "perm:" + userId);
        if (cached == null) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        for (String p : cached.split(",")) {
            if (!p.trim().isEmpty()) {
                set.add(p.trim());
            }
        }
        return set;
    }
}
