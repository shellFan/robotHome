package com.robot.home.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
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
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 接口文档配置：启动后访问 /swagger-ui/index.html 或 /doc.html
 */
@Configuration
@EnableOpenApi
@Profile("!prod")
public class SwaggerConfig {

    /**
     * Springfox 3.0.0 + Spring Boot Actuator 兼容性修复
     * Actuator 注册的 RequestMappingInfoHandlerMapping 使用 PathPatternParser，
     * Springfox 遍历时抛出异常。此 BeanPostProcessor 在 Springfox bean 创建后
     * 过滤掉使用 PathPatternParser 的 HandlerMapping，仅保留 AntPathMatcher 的。
     */
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private void customizeSpringfoxHandlerMappings(List<RequestMappingInfoHandlerMapping> mappings) {
                List<RequestMappingInfoHandlerMapping> copy = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(copy);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = WebMvcRequestHandlerProvider.class.getDeclaredField("handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

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
