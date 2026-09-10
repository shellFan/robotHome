package com.robot.home.ratelimit.aspect;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.ratelimit.entity.RateLimitConfig;
import com.robot.home.ratelimit.mapper.RateLimitConfigMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * <p>
 * 基于 Redis + rate_limit_config 表的可配置限流
 * 支持按 IP / USER / IP_USER 三种维度限流
 * <p>
 * Redis Key 设计:
 * - IP维度: robot:limit:{action}:ip:{ip}
 * - USER维度: robot:limit:{action}:user:{userId}
 * - IP_USER维度: robot:limit:{action}:iu:{ip}:{userId}
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /** 内存缓存: action → RateLimitConfig，5分钟刷新 */
    private final Map<String, RateLimitConfig> configCache = new ConcurrentHashMap<>();
    private volatile long configCacheTime = 0;
    private static final long CONFIG_CACHE_TTL_MS = 5 * 60 * 1000L;

    @Resource
    private RateLimitConfigMapper rateLimitConfigMapper;
    @Resource
    private RedisUtils redisUtils;

    @Around("@annotation(com.robot.home.ratelimit.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        String action = annotation.action();
        int windowSeconds;
        int maxRequests;
        String dimension;

        // 从DB读取配置（优先于注解默认值）
        RateLimitConfig config = getConfig(action);
        if (config != null && config.getEnabled() != null && config.getEnabled() == 1) {
            windowSeconds = config.getWindowSeconds();
            maxRequests = config.getMaxRequests();
            dimension = config.getDimension();
        } else {
            // 降级到注解默认值
            windowSeconds = annotation.windowSeconds();
            maxRequests = annotation.maxRequests();
            dimension = annotation.dimension();
        }

        // 构建限流key
        String limitKey = buildLimitKey(action, dimension);
        if (limitKey == null) {
            // USER维度但未登录，跳过限流
            return joinPoint.proceed();
        }

        // Redis计数器限流
        String countKey = Constants.CACHE_LIMIT_PREFIX + limitKey;
        Long current = redisUtils.increment(countKey);
        if (current != null && current == 1) {
            // 首次请求，设置过期时间
            redisUtils.expire(countKey, windowSeconds, TimeUnit.SECONDS);
        }

        if (current != null && current > maxRequests) {
            log.warn("限流触发: action={}, key={}, current={}, max={}", action, limitKey, current, maxRequests);
            throw new BusinessException("请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    /**
     * 构建限流key
     */
    private String buildLimitKey(String action, String dimension) {
        HttpServletRequest request = getRequest();
        String ip = getClientIp(request);
        Long userId = SecurityUtils.currentUserId();

        switch (dimension) {
            case "IP":
                return action + ":ip:" + ip;
            case "USER":
                if (userId == null) {
                    return null; // 未登录用户不限流
                }
                return action + ":user:" + userId;
            case "IP_USER":
                // IP + 用户组合维度，未登录时用IP
                return action + ":iu:" + ip + ":" + (userId != null ? userId : "anon");
            default:
                return action + ":ip:" + ip;
        }
    }

    /**
     * 获取DB配置（带内存缓存）
     */
    private RateLimitConfig getConfig(String action) {
        long now = System.currentTimeMillis();
        if (now - configCacheTime < CONFIG_CACHE_TTL_MS) {
            return configCache.get(action);
        }
        // 刷新缓存
        try {
            configCache.clear();
            java.util.List<RateLimitConfig> configs = rateLimitConfigMapper.selectList(
                    Wrappers.<RateLimitConfig>lambdaQuery().eq(RateLimitConfig::getEnabled, 1));
            for (RateLimitConfig c : configs) {
                configCache.put(c.getAction(), c);
            }
            configCacheTime = now;
        } catch (Exception e) {
            log.warn("加载限流配置失败: {}", e.getMessage());
            configCacheTime = now; // 避免频繁重试
        }
        return configCache.get(action);
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}