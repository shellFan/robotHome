package com.robot.home.ratelimit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * <p>
 * 使用方式: @RateLimit(action = "search")
 * action 对应 rate_limit_config 表的 action 字段
 * <p>
 * 如果 rate_limit_config 表中有对应配置且 enabled=1，则按配置限流
 * 如果没有配置，则使用注解上的默认值
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流接口标识（对应 rate_limit_config.action）
     */
    String action();

    /**
     * 默认时间窗口（秒），当DB无配置时使用
     */
    int windowSeconds() default 60;

    /**
     * 默认窗口内最大请求数，当DB无配置时使用
     */
    int maxRequests() default 30;

    /**
     * 默认限流维度: IP / USER / IP_USER
     */
    String dimension() default "IP";
}