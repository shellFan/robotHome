package com.robot.home.collector.fetcher;

import com.robot.home.collector.util.UrlSecurityUtil;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP抓取器
 * 基于Jsoup HTTP客户端，支持：请求限速、重试、代理、自定义Headers
 */
@Component
public class HttpFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpFetcher.class);

    /** 默认最大body大小 5MB */
    private static final int DEFAULT_MAX_BODY_SIZE = 5 * 1024 * 1024;

    private String userAgent;
    private int timeout;           // 总超时（兼容旧接口）
    private int connectTimeout;    // 连接超时(ms)
    private int readTimeout;       // 读取超时(ms)
    private int retryCount;
    private long retryDelay;
    private Proxy proxy;
    private Map<String, String> defaultHeaders = new ConcurrentHashMap<>();
    private Map<String, Long> lastFetchTime = new ConcurrentHashMap<>();
    private long minInterval;
    private int maxBodySize;       // 最大body大小(bytes)

    public HttpFetcher() {
        this("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
             30000, 3, 2000, 1000);
    }

    public HttpFetcher(String userAgent, int timeout, int retryCount, long retryDelay, long minInterval) {
        this.userAgent = userAgent;
        this.timeout = timeout;
        this.connectTimeout = timeout;   // 默认连接超时=总超时
        this.readTimeout = timeout;      // 默认读取超时=总超时
        this.retryCount = retryCount;
        this.retryDelay = retryDelay;
        this.minInterval = minInterval;
        this.maxBodySize = DEFAULT_MAX_BODY_SIZE;
        initDefaultHeaders();
    }

    private void initDefaultHeaders() {
        defaultHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/rss+xml;q=0.9,application/atom+xml;q=0.9,*/*;q=0.8");
        defaultHeaders.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7");
        defaultHeaders.put("Accept-Encoding", "gzip, deflate");
        defaultHeaders.put("Connection", "keep-alive");
        defaultHeaders.put("Cache-Control", "max-age=0");
        defaultHeaders.put("Upgrade-Insecure-Requests", "1");
        defaultHeaders.put("Sec-Fetch-Dest", "document");
        defaultHeaders.put("Sec-Fetch-Mode", "navigate");
        defaultHeaders.put("Sec-Fetch-Site", "none");
        defaultHeaders.put("Sec-Fetch-User", "?1");
    }

    /**
     * 抓取URL
     */
    public FetchResult fetch(String url) {
        return fetch(url, null);
    }

    /**
     * 抓取URL（带自定义Headers）
     */
    public FetchResult fetch(String url, Map<String, String> extraHeaders) {
        // SSRF防护：校验URL安全性
        if (!UrlSecurityUtil.isAllowedUrl(url)) {
            String reason = UrlSecurityUtil.getRejectionReason(url);
            log.warn("SSRF protection: fetch blocked for URL '{}': {}", url, reason);
            FetchResult result = new FetchResult();
            result.setUrl(url);
            result.setStatusCode(0);
            result.setError("SSRF protection: " + (reason != null ? reason : "URL not allowed"));
            return result;
        }

        // 限速
        enforceRateLimit(url);

        long startTime = System.currentTimeMillis();
        FetchResult result = new FetchResult();
        result.setUrl(url);

        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                Connection conn = buildConnection(url, extraHeaders);
                Connection.Response response = conn.execute();

                result.setStatusCode(response.statusCode());
                result.setContentType(response.contentType());
                result.setHtml(response.body());

                // 记录响应头
                for (Map.Entry<String, String> header : response.headers().entrySet()) {
                    result.addHeader(header.getKey(), header.getValue());
                }

                // 处理重定向
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String location = response.header("Location");
                    if (location != null) {
                        result.setRedirectedUrl(location);
                    }
                }

                long elapsed = System.currentTimeMillis() - startTime;
                result.setFetchTimeMs(elapsed);
                log.debug("Fetched {} -> {} ({}ms, attempt {})", url, response.statusCode(), elapsed, attempt);

                // 记录最后抓取时间
                lastFetchTime.put(getDomainKey(url), System.currentTimeMillis());

                return result;

            } catch (IOException e) {
                lastException = e;
                log.warn("Fetch attempt {}/{} failed for {}: {}", attempt, retryCount, url, e.getMessage());

                if (attempt < retryCount) {
                    try {
                        Thread.sleep(retryDelay * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 所有重试失败
        result.setError(lastException != null ? lastException.getMessage() : "Unknown error");
        result.setFetchTimeMs(System.currentTimeMillis() - startTime);
        log.error("All {} attempts failed for {}: {}", retryCount, url,
                lastException != null ? lastException.getMessage() : "unknown");
        return result;
    }

    /**
     * 抓取二进制内容（用于图片等非文本资源）
     */
    public FetchResult fetchBinary(String url) {
        return fetchBinary(url, null);
    }

    /**
     * 抓取二进制内容（带自定义Headers）
     */
    public FetchResult fetchBinary(String url, Map<String, String> extraHeaders) {
        enforceRateLimit(url);

        long startTime = System.currentTimeMillis();
        FetchResult result = new FetchResult();
        result.setUrl(url);

        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                Connection conn = buildConnection(url, extraHeaders);
                Connection.Response response = conn.execute();

                result.setStatusCode(response.statusCode());
                result.setContentType(response.contentType());
                result.setBody(response.bodyAsBytes());

                for (Map.Entry<String, String> header : response.headers().entrySet()) {
                    result.addHeader(header.getKey(), header.getValue());
                }

                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String location = response.header("Location");
                    if (location != null) {
                        result.setRedirectedUrl(location);
                    }
                }

                long elapsed = System.currentTimeMillis() - startTime;
                result.setFetchTimeMs(elapsed);
                log.debug("Fetched binary {} -> {} ({}ms, {} bytes, attempt {})",
                        url, response.statusCode(), elapsed,
                        result.getBody() != null ? result.getBody().length : 0, attempt);

                lastFetchTime.put(getDomainKey(url), System.currentTimeMillis());
                return result;

            } catch (IOException e) {
                lastException = e;
                log.warn("Binary fetch attempt {}/{} failed for {}: {}", attempt, retryCount, url, e.getMessage());

                if (attempt < retryCount) {
                    try {
                        Thread.sleep(retryDelay * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        result.setError(lastException != null ? lastException.getMessage() : "Unknown error");
        result.setFetchTimeMs(System.currentTimeMillis() - startTime);
        log.error("All {} binary fetch attempts failed for {}: {}", retryCount, url,
                lastException != null ? lastException.getMessage() : "unknown");
        return result;
    }

    /**
     * 抓取并解析为Jsoup Document
     */
    public Document fetchDocument(String url) throws IOException {
        return fetchDocument(url, null);
    }

    /**
     * 抓取并解析为Jsoup Document（带自定义Headers）
     */
    public Document fetchDocument(String url, Map<String, String> extraHeaders) throws IOException {
        // SSRF防护：校验URL安全性
        if (!UrlSecurityUtil.isAllowedUrl(url)) {
            String reason = UrlSecurityUtil.getRejectionReason(url);
            log.warn("SSRF protection: fetchDocument blocked for URL '{}': {}", url, reason);
            throw new IOException("SSRF protection: " + (reason != null ? reason : "URL not allowed"));
        }

        enforceRateLimit(url);

        Connection conn = buildConnection(url, extraHeaders);
        Document doc = conn.get();

        lastFetchTime.put(getDomainKey(url), System.currentTimeMillis());
        return doc;
    }

    /**
     * 检查URL是否可访问（HEAD请求）
     */
    public FetchResult head(String url) {
        // SSRF防护：校验URL安全性
        if (!UrlSecurityUtil.isAllowedUrl(url)) {
            String reason = UrlSecurityUtil.getRejectionReason(url);
            log.warn("SSRF protection: head blocked for URL '{}': {}", url, reason);
            FetchResult result = new FetchResult();
            result.setUrl(url);
            result.setStatusCode(0);
            result.setError("SSRF protection: " + (reason != null ? reason : "URL not allowed"));
            return result;
        }

        enforceRateLimit(url);

        FetchResult result = new FetchResult();
        result.setUrl(url);

        try {
            Connection conn = Jsoup.connect(url)
                    .method(Connection.Method.HEAD)
                    .userAgent(userAgent)
                    .timeout(timeout)
                    .followRedirects(true)
                    .ignoreHttpErrors(true);

            applyHeaders(conn, null);
            if (proxy != null) {
                conn.proxy(proxy);
            }

            Connection.Response response = conn.execute();
            result.setStatusCode(response.statusCode());
            result.setContentType(response.contentType());
            for (Map.Entry<String, String> header : response.headers().entrySet()) {
                result.addHeader(header.getKey(), header.getValue());
            }
        } catch (IOException e) {
            result.setError(e.getMessage());
        }

        lastFetchTime.put(getDomainKey(url), System.currentTimeMillis());
        return result;
    }

    /**
     * 构建Jsoup连接
     */
    private Connection buildConnection(String url, Map<String, String> extraHeaders) {
        Connection conn = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(timeout)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(maxBodySize);

        applyHeaders(conn, extraHeaders);

        if (proxy != null) {
            conn.proxy(proxy);
        }

        return conn;
    }

    /**
     * 应用请求头
     */
    private void applyHeaders(Connection conn, Map<String, String> extraHeaders) {
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            conn.header(header.getKey(), header.getValue());
        }
        if (extraHeaders != null) {
            for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
                conn.header(header.getKey(), header.getValue());
            }
        }
    }

    /**
     * 请求限速：确保同一域名两次请求间隔不小于minInterval
     */
    private void enforceRateLimit(String url) {
        String domainKey = getDomainKey(url);
        Long lastTime = lastFetchTime.get(domainKey);
        if (lastTime != null) {
            long elapsed = System.currentTimeMillis() - lastTime;
            if (elapsed < minInterval) {
                long wait = minInterval - elapsed;
                log.debug("Rate limiting: waiting {}ms for {}", wait, domainKey);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 提取域名作为限速key
     */
    private String getDomainKey(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    // Setters
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setTimeout(int timeout) { this.timeout = timeout; this.connectTimeout = timeout; this.readTimeout = timeout; }
    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }
    public void setMinInterval(long minInterval) { this.minInterval = minInterval; }
    public void setMaxBodySize(int maxBodySize) { this.maxBodySize = maxBodySize; }
    public void setAcceptLanguage(String acceptLanguage) { defaultHeaders.put("Accept-Language", acceptLanguage); }
    public void setProxy(String host, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }
    public void setProxy(Proxy proxy) { this.proxy = proxy; }
    public void addDefaultHeader(String key, String value) { defaultHeaders.put(key, value); }

    // Getters
    public String getUserAgent() { return userAgent; }
    public int getTimeout() { return timeout; }
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public int getMaxBodySize() { return maxBodySize; }
    public long getMinInterval() { return minInterval; }
}