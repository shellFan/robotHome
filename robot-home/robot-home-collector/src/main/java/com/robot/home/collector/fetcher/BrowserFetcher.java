package com.robot.home.collector.fetcher;

import com.robot.home.collector.util.UrlSecurityUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.ServiceWorkerPolicy;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 浏览器抓取器（Playwright）
 * 用于JS动态渲染页面，支持等待选择器、滚动加载等
 * Playwright依赖为provided scope，运行时需确保已安装
 */
@Component
public class BrowserFetcher {

    private static final Logger log = LoggerFactory.getLogger(BrowserFetcher.class);
    private static final Set<String> HOP_BY_HOP_RESPONSE_HEADERS = new HashSet<>(Arrays.asList(
            "connection", "content-length", "content-encoding", "keep-alive",
            "proxy-authenticate", "proxy-authorization", "proxy-connection", "te",
            "trailer", "transfer-encoding", "upgrade"));

    @Autowired
    private HttpFetcher httpFetcher;
    private Playwright playwright;
    private Browser browser;
    private BrowserEgressDenyProxy egressDenyProxy;
    private PlaywrightFactory playwrightFactory = Playwright::create;
    private boolean headless = true;
    private int timeout = 30000;
    private long minInterval = 2000;
    private Map<String, Long> lastFetchTime = new ConcurrentHashMap<>();

    public BrowserFetcher() {
    }

    public BrowserFetcher(boolean headless, int timeout) {
        this.headless = headless;
        this.timeout = timeout;
    }

    /**
     * 初始化浏览器实例
     */
    public synchronized void init() {
        if (playwright == null) {
            try {
                egressDenyProxy = new BrowserEgressDenyProxy();
                playwright = playwrightFactory.create();
                browser = playwright.chromium().launch(
                        createLaunchOptions(headless, egressDenyProxy.serverAddress()));
                log.info("BrowserFetcher initialized with Chromium, headless={}", headless);
            } catch (Exception e) {
                close();
                log.error("Failed to initialize Playwright browser. " +
                        "Ensure Playwright is installed: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install chromium\"", e);
                throw new RuntimeException("Browser initialization failed", e);
            }
        }
    }

    /**
     * 抓取动态渲染页面
     */
    public FetchResult fetch(String url) {
        return fetch(url, null, null);
    }

    /**
     * 抓取动态渲染页面（等待指定选择器）
     */
    public FetchResult fetch(String url, String waitForSelector, Integer waitAfterLoadMs) {
        long startTime = System.currentTimeMillis();
        FetchResult result = new FetchResult();
        result.setUrl(url);

        String rejectionReason = UrlSecurityUtil.getRejectionReason(url);
        if (rejectionReason != null) {
            result.setError("SSRF protection: " + rejectionReason);
            return result;
        }
        enforceRateLimit(url);

        if (browser == null) {
            init();
        }

        BrowserContext context = null;
        Page page = null;
        try {
            context = browser.newContext(createContextOptions(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
            installSsrFGuard(context);
            page = context.newPage();

            // 设置视口大小
            page.setViewportSize(1920, 1080);

            // 导航到URL
            page.navigate(url, new Page.NavigateOptions().setTimeout(timeout));

            // 等待页面加载
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // 等待指定选择器
            if (waitForSelector != null && !waitForSelector.isEmpty()) {
                page.waitForSelector(waitForSelector, new Page.WaitForSelectorOptions().setTimeout(timeout));
            }

            // 等待网络空闲
            try {
                page.waitForLoadState(LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(5000));
            } catch (Exception e) {
                log.debug("Network idle timeout (acceptable): {}", e.getMessage());
            }

            // 额外等待（用于延迟加载的内容）
            if (waitAfterLoadMs != null && waitAfterLoadMs > 0) {
                Thread.sleep(waitAfterLoadMs);
            }

            // 滚动页面触发懒加载
            page.evaluate("window.scrollTo(0, document.body.scrollHeight/2)");
            Thread.sleep(500);
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            Thread.sleep(500);

            // 获取HTML
            String html = page.content();
            result.setStatusCode(200);
            result.setHtml(html);
            result.setContentType("text/html");

            // 获取最终URL（可能被JS重定向）
            result.setRedirectedUrl(page.url());

            long elapsed = System.currentTimeMillis() - startTime;
            result.setFetchTimeMs(elapsed);
            log.debug("Browser fetched {} ({}ms)", url, elapsed);

        } catch (Exception e) {
            result.setError(e.getMessage());
            result.setFetchTimeMs(System.currentTimeMillis() - startTime);
            log.error("Browser fetch failed for {}: {}", url, e.getMessage());
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    log.warn("Failed to close browser context: {}", e.getMessage());
                }
            }
        }

        lastFetchTime.put(getDomainKey(url), System.currentTimeMillis());
        return result;
    }

    /**
     * 截图
     */
    public byte[] screenshot(String url) {
        if (UrlSecurityUtil.getRejectionReason(url) != null) {
            log.warn("SSRF protection: screenshot blocked for URL {}", url);
            return null;
        }
        if (browser == null) {
            init();
        }

        BrowserContext context = null;
        Page page = null;
        try {
            context = browser.newContext(createContextOptions(null));
            installSsrFGuard(context);
            page = context.newPage();
            page.navigate(url, new Page.NavigateOptions().setTimeout(timeout));
            page.waitForLoadState(LoadState.LOAD);
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (Exception e) {
            log.error("Screenshot failed for {}", url, e);
            return null;
        } finally {
            if (context != null) {
                try { context.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 请求限速
     */
    private void enforceRateLimit(String url) {
        String domainKey = getDomainKey(url);
        Long lastTime = lastFetchTime.get(domainKey);
        if (lastTime != null) {
            long elapsed = System.currentTimeMillis() - lastTime;
            if (elapsed < minInterval) {
                try {
                    Thread.sleep(minInterval - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private String getDomainKey(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    void installSsrFGuard(BrowserContext context) {
        context.route("**/*", route -> {
            Request request = route.request();
            String requestUrl = request.url();
            FetchResult fetched = httpFetcher.fetchBrowserRequest(requestUrl, request.method(),
                    request.allHeaders(), request.postDataBuffer());
            if (fetched.getError() != null || fetched.getStatusCode() <= 0) {
                log.warn("SSRF-safe browser transport blocked URL {}: {}", requestUrl, fetched.getError());
                route.abort();
                return;
            }

            Map<String, String> headers = new HashMap<>();
            for (Map.Entry<String, String> header : fetched.getHeaders().entrySet()) {
                if (!HOP_BY_HOP_RESPONSE_HEADERS.contains(header.getKey().toLowerCase(Locale.ROOT))) {
                    headers.put(header.getKey(), header.getValue());
                }
            }
            if (fetched.getContentType() != null && !headers.containsKey("Content-Type")) {
                headers.put("Content-Type", fetched.getContentType());
            }
            byte[] responseBody = fetched.getBody() != null
                    ? fetched.getBody()
                    : new byte[0];
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(fetched.getStatusCode())
                    .setHeaders(headers)
                    .setBodyBytes(responseBody));
        });
    }

    /**
     * 关闭浏览器
     */
    public synchronized void close() {
        if (browser != null) {
            try { browser.close(); } catch (Exception ignored) {}
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
        }
        closeEgressDenyProxy();
        browser = null;
        playwright = null;
        log.info("BrowserFetcher closed");
    }

    public boolean isAvailable() {
        return browser != null;
    }

    static BrowserType.LaunchOptions createLaunchOptions(boolean headless, String proxyAddress) {
        return new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setProxy(new Proxy(proxyAddress).setBypass("<-loopback>"))
                .setArgs(Arrays.asList(
                        "--host-resolver-rules=MAP * ~NOTFOUND",
                        "--disable-background-networking",
                        "--disable-quic",
                        "--force-webrtc-ip-handling-policy=disable_non_proxied_udp"));
    }

    static Browser.NewContextOptions createContextOptions(String userAgent) {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setServiceWorkers(ServiceWorkerPolicy.BLOCK)
                .setViewportSize(1920, 1080);
        if (userAgent != null) {
            options.setUserAgent(userAgent);
        }
        return options;
    }

    @PreDestroy
    private void shutdown() {
        close();
    }

    private void closeEgressDenyProxy() {
        if (egressDenyProxy != null) {
            try {
                egressDenyProxy.close();
            } catch (Exception e) {
                log.warn("Failed to close browser egress deny proxy: {}", e.getMessage());
            }
            egressDenyProxy = null;
        }
    }

    public void setHeadless(boolean headless) { this.headless = headless; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public void setMinInterval(long minInterval) { this.minInterval = minInterval; }

    void setPlaywrightFactory(PlaywrightFactory playwrightFactory) {
        if (playwrightFactory == null) {
            throw new IllegalArgumentException("playwrightFactory is required");
        }
        this.playwrightFactory = playwrightFactory;
    }

    interface PlaywrightFactory {
        Playwright create();
    }
}
