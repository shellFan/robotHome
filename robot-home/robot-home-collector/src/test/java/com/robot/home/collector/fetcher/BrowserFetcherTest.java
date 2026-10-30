package com.robot.home.collector.fetcher;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.ArgumentCaptor;
import com.microsoft.playwright.options.ServiceWorkerPolicy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserFetcherTest {

    @Test
    void blocksPrivateAddressBeforeStartingBrowser() {
        BrowserFetcher fetcher = new BrowserFetcher(true, 1000);

        FetchResult result = fetcher.fetch("http://127.0.0.1/admin");

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("SSRF"));
        assertFalse(fetcher.isAvailable());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fulfillsBrowserRequestsThroughThePinnedHttpTransport() throws Exception {
        String requestUrl = "https://assets.example.invalid/app.js";
        Map<String, String> requestHeaders = Collections.singletonMap("accept", "*/*");
        HttpFetcher safeTransport = mock(HttpFetcher.class);
        FetchResult fetched = new FetchResult();
        fetched.setStatusCode(200);
        fetched.setContentType("application/javascript");
        fetched.setBody(new byte[]{1, 2, 3});
        when(safeTransport.fetchBrowserRequest(requestUrl, "GET", requestHeaders, null))
                .thenReturn(fetched);

        BrowserFetcher browserFetcher = new BrowserFetcher(true, 1000);
        Field transportField = BrowserFetcher.class.getDeclaredField("httpFetcher");
        transportField.setAccessible(true);
        transportField.set(browserFetcher, safeTransport);

        BrowserContext context = mock(BrowserContext.class);
        Method installGuard = BrowserFetcher.class.getDeclaredMethod("installSsrFGuard", BrowserContext.class);
        installGuard.setAccessible(true);
        installGuard.invoke(browserFetcher, context);

        ArgumentCaptor<Consumer<Route>> handler = ArgumentCaptor.forClass((Class) Consumer.class);
        verify(context).route(eq("**/*"), handler.capture());

        Route route = mock(Route.class);
        Request request = mock(Request.class);
        when(route.request()).thenReturn(request);
        when(request.url()).thenReturn(requestUrl);
        when(request.method()).thenReturn("GET");
        when(request.allHeaders()).thenReturn(requestHeaders);
        when(request.postDataBuffer()).thenReturn(null);

        handler.getValue().accept(route);

        verify(safeTransport).fetchBrowserRequest(requestUrl, "GET", requestHeaders, null);
        verify(route).fulfill(any(Route.FulfillOptions.class));
        verify(route, never()).resume();
    }

    @Test
    void launchesWithFailClosedBrowserEgressAndServiceWorkersDisabled() throws Exception {
        try (BrowserEgressDenyProxy proxy = new BrowserEgressDenyProxy()) {
            BrowserType.LaunchOptions launch = BrowserFetcher.createLaunchOptions(true, proxy.serverAddress());
            Browser.NewContextOptions context = BrowserFetcher.createContextOptions("test-agent");

            assertTrue(launch.args.contains("--host-resolver-rules=MAP * ~NOTFOUND"));
            assertTrue(launch.args.contains("--disable-quic"));
            assertTrue(launch.args.contains("--force-webrtc-ip-handling-policy=disable_non_proxied_udp"));
            assertEquals(proxy.serverAddress(), launch.proxy.server);
            assertTrue(launch.proxy.bypass.contains("<-loopback>"));
            assertEquals(ServiceWorkerPolicy.BLOCK, context.serviceWorkers);
        }
    }

    @Test
    void closesPlaywrightAndDenyProxyWhenBrowserLaunchFails() {
        BrowserFetcher browserFetcher = new BrowserFetcher(true, 1000);
        com.microsoft.playwright.Playwright playwright = mock(com.microsoft.playwright.Playwright.class);
        BrowserType chromium = mock(BrowserType.class);
        when(playwright.chromium()).thenReturn(chromium);
        when(chromium.launch(any(BrowserType.LaunchOptions.class)))
                .thenThrow(new RuntimeException("simulated launch failure"));
        browserFetcher.setPlaywrightFactory(() -> playwright);

        assertThrows(RuntimeException.class, browserFetcher::init);

        verify(playwright).close();
        assertFalse(browserFetcher.isAvailable());
    }
}
