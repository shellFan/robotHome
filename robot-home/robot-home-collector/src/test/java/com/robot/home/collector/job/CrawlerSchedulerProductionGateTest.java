package com.robot.home.collector.job;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CrawlerSchedulerProductionGateTest {

    @Test
    void requiresExplicitOptInBeforeStartingScheduledExternalCrawls() {
        ConditionalOnProperty condition = CrawlerJob.class.getAnnotation(ConditionalOnProperty.class);

        assertNotNull(condition, "scheduled crawler must be behind a production-safe property gate");
        assertEquals("crawler.scheduler", condition.prefix());
        assertEquals("enabled", condition.name()[0]);
        assertEquals("true", condition.havingValue());
        assertFalse(condition.matchIfMissing());
    }
}
