package com.robot.home.collector.fetcher;

import com.robot.home.collector.config.CrawlerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpFetcherConfigurationTest {

    @Test
    void appliesCrawlerRetryRateLimitAndTimeoutSettings() {
        CrawlerConfig config = new CrawlerConfig();
        config.setUserAgent("RobotHomeCrawler/Test");
        config.setMaxRetries(4);
        config.setRetryBaseInterval(250);
        config.setMinInterval(120);
        config.setConnectTimeout(1700);
        config.setReadTimeout(1200);

        HttpFetcher fetcher = new HttpFetcher(config);

        assertEquals("RobotHomeCrawler/Test", fetcher.getUserAgent());
        assertEquals(4, fetcher.getRetryCount());
        assertEquals(250L, fetcher.getRetryDelay());
        assertEquals(120L, fetcher.getMinInterval());
        assertEquals(1700, fetcher.getConnectTimeout());
        assertEquals(1200, fetcher.getReadTimeout());
        assertEquals(1700, fetcher.getTimeout());
    }
}
