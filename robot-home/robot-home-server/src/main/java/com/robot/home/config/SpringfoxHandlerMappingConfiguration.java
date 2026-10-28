package com.robot.home.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Keeps Springfox 3 compatible with Spring Boot's Actuator mappings.
 * This must also run in prod because the Springfox starter's bootstrapper is
 * still created there even though the application's Docket is disabled.
 */
@Configuration
public class SpringfoxHandlerMappingConfiguration {

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
}
