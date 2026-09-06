package com.robot.home.collector.config;

import com.robot.home.collector.security.ApiKeyInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 采集器 Web 配置：API Key 拦截器、CORS
 */
@Configuration
public class CollectorWebMvcConfig implements WebMvcConfigurer {

    @Resource
    private ApiKeyInterceptor apiKeyInterceptor;

    @Value("${cors.allowed-origins:http://localhost:8080,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 所有采集器 API 需要 API Key 认证（/health 端点除外）
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/crawler/**")
                .excludePathPatterns("/health");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}