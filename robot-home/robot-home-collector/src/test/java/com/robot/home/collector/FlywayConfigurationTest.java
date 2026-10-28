package com.robot.home.collector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Collector Flyway configuration")
class FlywayConfigurationTest {

    @Test
    @DisplayName("collector owns an isolated Flyway history table")
    void collectorUsesDedicatedFlywayHistoryTable() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("collector-application", new ClassPathResource("application.yml"));

        Object configuredTable = null;
        for (PropertySource<?> source : sources) {
            Object value = source.getProperty("spring.flyway.table");
            if (value != null) {
                configuredTable = value;
                break;
            }
        }

        assertEquals("collector_flyway_schema_history", configuredTable,
                "Server and Collector must not share flyway_schema_history");
    }
}
