package com.robot.home.collector.fetcher;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 浏览器抓取器（Playwright）
 * 用于JS动态渲染页面，支持等待选择器、滚动加载等
 * Playwright依赖为provided scope，运行时需确保已安装
 */
@Component
public class BrowserFetcher {

    private static final Logger log = LoggerFactory.getLogger(BrowserFetcher.class);

    private Playwright playwright;
    private Browser browser;
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
                playwright = Playwright.create();
                browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(headless));
                log.info("BrowserFetcher initialized with Chromium, headless={}", headless);
            } catch (Exception e) {
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
        enforceRateLimit(url);

        long startTime = System.currentTimeMillis();
        FetchResult result = new FetchResult();
        result.setUrl(url);

        if (browser == null) {
            init();
        }

        Page page = null;
        try {
            page = browser.newPage(new Browser.NewPageOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

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
            if (page != null) {
                try {
                    page.close();
                } catch (Exception e) {
                    log.warn("Failed to close page: {}", e.getMessage());
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
        if (browser == null) {
            init();
        }

        Page page = null;
        try {
            page = browser.newPage();
            page.navigate(url, new Page.NavigateOptions().setTimeout(timeout));
            page.waitForLoadState(LoadState.LOAD);
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (Exception e) {
            log.error("Screenshot failed for {}", url, e);
            return null;
        } finally {
            if (page != null) {
                try { page.close(); } catch (Exception ignored) {}
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
        browser = null;
        playwright = null;
        log.info("BrowserFetcher closed");
    }

    public boolean isAvailable() {
        return browser != null;
    }

    public void setHeadless(boolean headless) { this.headless = headless; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public void setMinInterval(long minInterval) { this.minInterval = minInterval; }
}