package com.robot.home.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringfoxProductionCompatibilityTest {

    @Test
    void keepsPathPatternCompatibilityPostProcessorActiveInProduction() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        try {
            ConfigurableEnvironment environment = context.getEnvironment();
            environment.setActiveProfiles("prod");
            context.register(SpringfoxHandlerMappingConfiguration.class, SwaggerConfig.class);
            context.refresh();

            assertTrue(context.containsBean("springfoxHandlerProviderBeanPostProcessor"),
                    "Springfox must filter PathPattern handler mappings even when API docs are disabled");
        } finally {
            context.close();
        }
    }
}
