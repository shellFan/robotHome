package com.robot.home.common.aspect;

import com.robot.home.common.util.RedisUtils;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 访问统计切面：按天统计 PV / UV，写入 Redis（HyperLogLog 不适用于小规模场景，此处用 Set 精确去重）
 */
@Aspect
@Component
public class VisitAspect {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private HttpServletRequest request;

    @AfterReturning("execution(* com.robot.home..controller..*(..))")
    public void track() {
        String uri = request == null ? null : request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return;
        }
        String day = LocalDate.now().format(FMT);
        try {
            redisUtils.increment("robot:stat:pv:" + day);
            String ip = clientIp();
            if (ip != null) {
                redisUtils.sAdd("robot:stat:uv:" + day, ip);
            }
        } catch (Exception ignored) {
            // 统计失败不影响主流程
        }
    }

    private String clientIp() {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
