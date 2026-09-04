package com.robot.home.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;

/**
 * 接口文档配置：启动后访问 /swagger-ui/index.html 或 /doc.html
 */
@Configuration
@EnableOpenApi
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.robot.home"))
                .paths(PathSelectors.ant("/api/**"))
                .build()
                .securitySchemes(Collections.singletonList(new ApiKey("Authorization", "Authorization", "header")))
                .securityContexts(Collections.singletonList(securityContext()));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("机器人之家 API 文档")
                .description("机器人之家 · 机器人行业垂直平台（PC / 小程序 / 管理后台统一接口）")
                .version("1.0.0")
                .build();
    }

    private SecurityContext securityContext() {
        AuthorizationScope scope = new AuthorizationScope("global", "全局访问");
        SecurityReference reference = new SecurityReference("Authorization", new AuthorizationScope[]{scope});
        return SecurityContext.builder()
                .securityReferences(Collections.singletonList(reference))
                .forPaths(PathSelectors.any())
                .build();
    }
}
