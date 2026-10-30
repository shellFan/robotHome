package com.robot.home.collector.fetcher;

import org.junit.jupiter.api.Test;
import com.robot.home.collector.util.SafeDnsResolver;

import java.net.InetAddress;
import java.net.Proxy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import okhttp3.OkHttpClient;

class HttpFetcherTest {

    @Test
    void disablesUnmanagedSystemProxyForDnsPinnedRequests() throws Exception {
        HttpFetcher fetcher = new HttpFetcher();
        Field clientField = HttpFetcher.class.getDeclaredField("httpClient");
        clientField.setAccessible(true);
        OkHttpClient client = (OkHttpClient) clientField.get(fetcher);

        assertEquals(Proxy.NO_PROXY, client.proxy(),
                "the pinned destination DNS must not be bypassed through a JVM proxy selector");
    }

    @Test
    void doesNotForwardCallerHeadersAcrossOrigins() throws Exception {
        Method policy = HttpFetcher.class.getDeclaredMethod(
                "shouldForwardHeaders", String.class, String.class);
        policy.setAccessible(true);

        assertTrue(!(Boolean) policy.invoke(null,
                "https://source.example/path", "https://other.example/next"),
                "Caller headers must be stripped on a cross-origin redirect");
        assertTrue((Boolean) policy.invoke(null,
                "https://source.example/path", "https://source.example:443/next"),
                "Equivalent same-origin URLs may keep caller headers");
        assertTrue(!(Boolean) policy.invoke(null,
                "https://source.example/path", "http://source.example/next"),
                "A scheme change is a different origin");
    }

    @Test
    void rejectsPrivateDnsRebindingAtTheActualConnectionResolver() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        SafeDnsResolver connectionResolver = new SafeDnsResolver(host -> {
            lookups.incrementAndGet();
            return new InetAddress[]{InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})};
        });
        HttpFetcher fetcher = new HttpFetcher("test-agent", 1000, 1, 0, 0, connectionResolver);

        FetchResult result = fetcher.fetch("http://8.8.8.8/");

        assertTrue(!result.isSuccess());
        assertTrue(result.getError().contains("private"));
        assertTrue(lookups.get() == 1, "the connection must use one validated DNS answer");
    }

    @Test
    void serializesConcurrentRateLimitReservationsForSameHost() throws Exception {
        HttpFetcher fetcher = new HttpFetcher();
        fetcher.setMinInterval(120);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> invokeRateLimit(fetcher, ready, start));
            Future<?> second = executor.submit(() -> invokeRateLimit(fetcher, ready, start));

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            long began = System.nanoTime();
            start.countDown();
            first.get(3, TimeUnit.SECONDS);
            second.get(3, TimeUnit.SECONDS);

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began);
            assertTrue(elapsedMillis >= 80,
                    "Concurrent requests to one host should reserve separate rate-limit slots");
        } finally {
            executor.shutdownNow();
        }
    }

    private void invokeRateLimit(HttpFetcher fetcher, CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await();
            Method method = HttpFetcher.class.getDeclaredMethod("enforceRateLimit", String.class);
            method.setAccessible(true);
            method.invoke(fetcher, "https://example.invalid/robots");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
