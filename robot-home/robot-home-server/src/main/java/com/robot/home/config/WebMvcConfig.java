package com.robot.home.config;

import com.robot.home.security.AdminInterceptor;
import com.robot.home.security.JwtFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.io.File;

/**
 * Web 配置：拦截器、CORS、本地文件静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtFilter jwtFilter;

    @Resource
    private AdminInterceptor adminInterceptor;

    @Value("${file.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${cors.allowed-origins:http://localhost:8080,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 认证（放行登录注册等接口）
        registry.addInterceptor(jwtFilter)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/send-sms-code",
                        "/api/auth/captcha",
                        "/api/auth/refresh",
                        "/api/admin/auth/login",
                        "/error",
                        "/files/**",
                        "/uploads/**");

        // 后台管理权限
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = new File(uploadDir).getAbsolutePath();
        if (!dir.endsWith(File.separator)) {
            dir = dir + File.separator;
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + dir);
    }
}
