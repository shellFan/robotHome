package com.robot.home.collector.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 采集器 API Key 认证拦截器
 * 所有 /api/crawler/** 请求需携带 X-API-Key 请求头
 * <p>
 * 安全策略：
 * - 生产模式(dev-mode=false): 必须配置 crawler.api-key，否则启动失败
 * - 开发模式(dev-mode=true): 允许跳过认证，仅用于本地调试
 * - 仅支持 X-API-Key 请求头，禁止 URL 参数传递密钥
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyInterceptor.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${crawler.api-key:}")
    private String configuredApiKey;

    @Value("${crawler.dev-mode:false}")
    private boolean devMode;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void validateConfig() {
        if (devMode) {
            log.warn("======= 采集器运行在开发模式，API Key 认证已禁用！切勿在生产环境使用 =======");
        } else if (!StringUtils.hasText(configuredApiKey)) {
            throw new IllegalStateException(
                "生产环境必须配置 crawler.api-key！如需本地开发，请设置 crawler.dev-mode=true");
        }
        if (StringUtils.hasText(configuredApiKey)) {
            log.info("API Key authentication enabled, key prefix: {}***",
                configuredApiKey.substring(0, Math.min(4, configuredApiKey.length())));
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 开发模式跳过认证
        if (devMode) {
            return true;
        }

        // 仅从请求头获取 API Key（禁止 URL 参数传递密钥）
        String requestKey = request.getHeader(API_KEY_HEADER);

        if (requestKey == null || !configuredApiKey.equals(requestKey)) {
            log.warn("API key validation failed for request: {} {} from {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无效的API密钥，请在请求头中提供有效的 X-API-Key");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }

        return true;
    }
}