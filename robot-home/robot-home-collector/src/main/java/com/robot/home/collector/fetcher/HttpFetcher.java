package com.robot.home.collector.fetcher;

import com.robot.home.collector.config.CrawlerConfig;
import com.robot.home.collector.util.SafeDnsResolver;
import com.robot.home.collector.util.UrlSecurityUtil;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import okio.Buffer;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * HTTP抓取器
 * 基于Jsoup HTTP客户端，支持：请求限速、重试、代理、自定义Headers
 */
@Component
public class HttpFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpFetcher.class);

    /** 默认最大body大小 5MB */
    private static final int DEFAULT_MAX_BODY_SIZE = 5 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;
    private static final Set<String> HOP_BY_HOP_HEADERS = new HashSet<>(Arrays.asList(
            "connection", "content-length", "host", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "proxy-connection", "te", "trailer", "transfer-encoding",
            "upgrade", "accept-encoding"));
    private final OkHttpClient httpClient;

    private String userAgent;
    private int timeout;           // 总超时（兼容旧接口）
    private int connectTimeout;    // 连接超时(ms)
    private int readTimeout;       // 读取超时(ms)
    private int retryCount;
    private long retryDelay;
    private Proxy proxy;
    private Map<String, String> defaultHeaders = new ConcurrentHashMap<>();
    private Map<String, Long> lastFetchTime = new ConcurrentHashMap<>();
    private Map<String, Object> rateLimitLocks = new ConcurrentHashMap<>();
    private long minInterval;
    private int maxBodySize;       // 最大body大小(bytes)

    public HttpFetcher() {
        this("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
             30000, 3, 2000, 1000, new SafeDnsResolver());
    }

    @Autowired
    public HttpFetcher(CrawlerConfig crawlerConfig) {
        this(crawlerConfig.getUserAgent(), crawlerConfig.getReadTimeout(),
                crawlerConfig.getMaxRetries(), crawlerConfig.getRetryBaseInterval(), crawlerConfig.getMinInterval(),
                new SafeDnsResolver());
        this.connectTimeout = crawlerConfig.getConnectTimeout();
        this.readTimeout = crawlerConfig.getReadTimeout();
        // Jsoup exposes one combined connect/read timeout; do not cut either configured limit short.
        this.timeout = Math.max(connectTimeout, readTimeout);
    }

    public HttpFetcher(String userAgent, int timeout, int retryCount, long retryDelay, long minInterval) {
        this(userAgent, timeout, retryCount, retryDelay, minInterval, new SafeDnsResolver());
    }

    HttpFetcher(String userAgent, int timeout, int retryCount, long retryDelay, long minInterval,
                SafeDnsResolver dnsResolver) {
        this.userAgent = userAgent;
        this.timeout = timeout;
        this.connectTimeout = timeout;   // 默认连接超时=总超时
        this.readTimeout = timeout;      // 默认读取超时=总超时
        this.retryCount = retryCount;
        this.retryDelay = retryDelay;
        this.minInterval = minInterval;
        this.maxBodySize = DEFAULT_MAX_BODY_SIZE;
        this.httpClient = new OkHttpClient.Builder()
                .dns(dnsResolver)
                .proxy(Proxy.NO_PROXY)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        initDefaultHeaders();
    }

    private void initDefaultHeaders() {
        defaultHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/rss+xml;q=0.9,application/atom+xml;q=0.9,*/*;q=0.8");
        defaultHeaders.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7");
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
                FetchedResponse response = executeWithSafeRedirects(url, extraHeaders, "GET");

                result.setStatusCode(response.statusCode);
                result.setContentType(response.contentType);
                result.setHtml(response.text());
                setRedirectedUrl(result, url, response);

                copyHeaders(result, response.headers);

                long elapsed = System.currentTimeMillis() - startTime;
                result.setFetchTimeMs(elapsed);
                log.debug("Fetched {} -> {} ({}ms, attempt {})", url, response.statusCode, elapsed, attempt);

                // 记录最后抓取时间
                lastFetchTime.put(getDomainKey(url), System.currentTimeMillis());

                return result;

            } catch (UnsafeRedirectException e) {
                result.setError(e.getMessage());
                result.setFetchTimeMs(System.currentTimeMillis() - startTime);
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
     * Fetch one browser request through the DNS-pinned transport. Redirect
     * responses are returned to Playwright, which submits the next hop through
     * this method and therefore re-applies URL and DNS checks.
     */
    FetchResult fetchBrowserRequest(String url, String method,
                                    Map<String, String> requestHeaders, byte[] requestBody) {
        long startTime = System.currentTimeMillis();
        FetchResult result = new FetchResult();
        result.setUrl(url);

        String rejectionReason = UrlSecurityUtil.getRejectionReason(url);
        if (rejectionReason != null) {
            result.setError("SSRF protection: " + rejectionReason);
            return result;
        }
        if (proxy != null) {
            result.setError("SSRF protection: configured proxy does not guarantee pinned destination DNS");
            return result;
        }

        String safeMethod = method == null ? "" : method.toUpperCase(Locale.ROOT);
        if (!("GET".equals(safeMethod) || "HEAD".equals(safeMethod)
                || "POST".equals(safeMethod) || "PUT".equals(safeMethod)
                || "PATCH".equals(safeMethod) || "DELETE".equals(safeMethod)
                || "OPTIONS".equals(safeMethod))) {
            result.setError("Browser request method is not allowed");
            return result;
        }
        if (requestBody != null && requestBody.length > maxBodySize) {
            result.setError("Browser request body exceeds configured maximum size");
            return result;
        }

        enforceRateLimit(url);
        try {
            Request request = buildRequest(url, filterBrowserHeaders(requestHeaders),
                    false, safeMethod, requestBody);
            OkHttpClient client = httpClient.newBuilder()
                    .connectTimeout(Math.max(1, connectTimeout), TimeUnit.MILLISECONDS)
                    .readTimeout(Math.max(1, readTimeout), TimeUnit.MILLISECONDS)
                    .callTimeout(Math.max(1, timeout), TimeUnit.MILLISECONDS)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                byte[] body = readBoundedBody(responseBody);
                MediaType mediaType = responseBody != null ? responseBody.contentType() : null;
                Charset charset = mediaType != null ? mediaType.charset() : null;
                FetchedResponse fetched = new FetchedResponse(response.code(),
                        response.header("Content-Type"), body, response.headers(), url, charset);
                result.setStatusCode(fetched.statusCode);
                result.setContentType(fetched.contentType);
                result.setBody(fetched.body);
                copyHeaders(result, fetched.headers);
            }
        } catch (IOException | IllegalArgumentException e) {
            result.setError(e.getMessage());
        }
        result.setFetchTimeMs(System.currentTimeMillis() - startTime);
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
        // SSRF防护：校验URL安全性
        try {
            UrlSecurityUtil.validateUrl(url);
        } catch (SecurityException e) {
            log.warn("SSRF protection: fetchBinary blocked for URL '{}': {}", url, e.getMessage());
            FetchResult result = new FetchResult();
            result.setUrl(url);
            result.setError("SSRF protection: " + e.getMessage());
            return result;
        }

        enforceRateLimit(url);

        long startTime = System.currentTimeMillis();
        FetchResult result = new FetchResult();
        result.setUrl(url);

        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                FetchedResponse response = executeWithSafeRedirects(url, extraHeaders, "GET");

                result.setStatusCode(response.statusCode);
                result.setContentType(response.contentType);
                result.setBody(response.body);
                setRedirectedUrl(result, url, response);

                copyHeaders(result, response.headers);

                long elapsed = System.currentTimeMillis() - startTime;
                result.setFetchTimeMs(elapsed);
                log.debug("Fetched binary {} -> {} ({}ms, {} bytes, attempt {})",
                        url, response.statusCode, elapsed,
                        result.getBody() != null ? result.getBody().length : 0, attempt);

                lastFetchTime.put(getDomainKey(url), System.currentTimeMillis());
                return result;

            } catch (UnsafeRedirectException e) {
                result.setError(e.getMessage());
                result.setFetchTimeMs(System.currentTimeMillis() - startTime);
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

        FetchedResponse response = executeWithSafeRedirects(url, extraHeaders, "GET");
        Document doc = Jsoup.parse(new ByteArrayInputStream(response.body),
                response.declaredCharset != null ? response.declaredCharset.name() : null, response.url);

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
            FetchedResponse response = executeWithSafeRedirects(url, null, "HEAD");
            result.setStatusCode(response.statusCode);
            result.setContentType(response.contentType);
            setRedirectedUrl(result, url, response);
            copyHeaders(result, response.headers);
        } catch (UnsafeRedirectException e) {
            result.setError(e.getMessage());
        } catch (IOException e) {
            result.setError(e.getMessage());
        }

        lastFetchTime.put(getDomainKey(url), System.currentTimeMillis());
        return result;
    }

    private FetchedResponse executeWithSafeRedirects(String initialUrl,
                                                      Map<String, String> extraHeaders,
                                                      String method) throws IOException {
        if (proxy != null) {
            throw new UnsafeRedirectException(
                    "SSRF protection: configured proxy does not guarantee pinned destination DNS");
        }

        OkHttpClient client = httpClient.newBuilder()
                .connectTimeout(Math.max(1, connectTimeout), TimeUnit.MILLISECONDS)
                .readTimeout(Math.max(1, readTimeout), TimeUnit.MILLISECONDS)
                .callTimeout(Math.max(1, timeout), TimeUnit.MILLISECONDS)
                .build();
        String currentUrl = initialUrl;
        boolean forwardCallerHeaders = true;
        for (int redirectCount = 0; ; redirectCount++) {
            if (redirectCount > 0) {
                enforceRateLimit(currentUrl);
            }
            Request request = buildRequest(currentUrl,
                    forwardCallerHeaders ? extraHeaders : null, forwardCallerHeaders, method);
            try (Response response = client.newCall(request).execute()) {
                lastFetchTime.put(getDomainKey(currentUrl), System.currentTimeMillis());
                int status = response.code();
                String location = response.header("Location");
                if (status < 300 || status >= 400 || location == null || location.trim().isEmpty()) {
                    ResponseBody responseBody = response.body();
                    byte[] body = readBoundedBody(responseBody);
                    MediaType mediaType = responseBody != null ? responseBody.contentType() : null;
                    Charset charset = mediaType != null ? mediaType.charset() : null;
                    return new FetchedResponse(status, response.header("Content-Type"), body,
                            response.headers(), currentUrl, charset);
                }
                if (redirectCount >= MAX_REDIRECTS) {
                    throw new UnsafeRedirectException("Redirect limit exceeded");
                }
                try {
                    String redirectUrl = UrlSecurityUtil.resolveAndValidateRedirect(currentUrl, location);
                    if (!shouldForwardHeaders(currentUrl, redirectUrl)) {
                        // Drop caller and default headers after crossing origins.
                        forwardCallerHeaders = false;
                    }
                    currentUrl = redirectUrl;
                } catch (Exception e) {
                    throw new UnsafeRedirectException("SSRF protection: blocked unsafe redirect", e);
                }
            }
        }
    }

    private Request buildRequest(String url, Map<String, String> extraHeaders,
                                 boolean includeDefaultHeaders, String method) {
        return buildRequest(url, extraHeaders, includeDefaultHeaders, method, null);
    }

    private Request buildRequest(String url, Map<String, String> extraHeaders,
                                 boolean includeDefaultHeaders, String method, byte[] requestBody) {
        String contentTypeHeader = findHeader(extraHeaders, "Content-Type");
        RequestBody body = null;
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)
                || ("DELETE".equals(method) && requestBody != null)) {
            body = RequestBody.create(
                    contentTypeHeader != null ? MediaType.parse(contentTypeHeader) : null,
                    requestBody != null ? requestBody : new byte[0]);
        }

        Request.Builder builder = new Request.Builder().url(url);
        if (findHeader(extraHeaders, "User-Agent") == null) {
            builder.header("User-Agent", userAgent);
        }
        builder.method(method, body);
        if (includeDefaultHeaders) {
            applyHeaders(builder, defaultHeaders);
        }
        if (extraHeaders != null) {
            applyHeaders(builder, extraHeaders);
        }
        return builder.build();
    }

    private static Map<String, String> filterBrowserHeaders(Map<String, String> requestHeaders) {
        Map<String, String> filtered = new ConcurrentHashMap<>();
        if (requestHeaders == null) {
            return filtered;
        }
        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            String name = header.getKey();
            if (name != null && header.getValue() != null
                    && !HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                filtered.put(name, header.getValue());
            }
        }
        return filtered;
    }

    private static String findHeader(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey() != null && header.getKey().equalsIgnoreCase(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    private static void applyHeaders(Request.Builder builder, Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
    }

    private byte[] readBoundedBody(ResponseBody responseBody) throws IOException {
        if (responseBody == null) {
            return new byte[0];
        }
        long contentLength = responseBody.contentLength();
        if (contentLength > maxBodySize) {
            throw new IOException("Response body exceeds configured maximum size");
        }

        BufferedSource source = responseBody.source();
        Buffer buffer = new Buffer();
        long total = 0;
        long read;
        while ((read = source.read(buffer, 8192)) != -1) {
            total += read;
            if (total > maxBodySize) {
                throw new IOException("Response body exceeds configured maximum size");
            }
        }
        return buffer.readByteArray();
    }

    static boolean shouldForwardHeaders(String sourceUrl, String targetUrl) {
        try {
            URI source = new URI(sourceUrl);
            URI target = new URI(targetUrl);
            String sourceScheme = source.getScheme();
            String targetScheme = target.getScheme();
            String sourceHost = source.getHost();
            String targetHost = target.getHost();
            return sourceScheme != null && sourceScheme.equalsIgnoreCase(targetScheme)
                    && sourceHost != null && sourceHost.equalsIgnoreCase(targetHost)
                    && effectivePort(source) == effectivePort(target);
        } catch (URISyntaxException | IllegalArgumentException e) {
            return false;
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return -1;
    }

    private void setRedirectedUrl(FetchResult result, String initialUrl, FetchedResponse response) {
        if (!initialUrl.equals(response.url)) {
            result.setRedirectedUrl(response.url);
        }
    }

    private static final class UnsafeRedirectException extends IOException {
        private UnsafeRedirectException(String message) {
            super(message);
        }

        private UnsafeRedirectException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class FetchedResponse {
        private final int statusCode;
        private final String contentType;
        private final byte[] body;
        private final Headers headers;
        private final String url;
        private final Charset declaredCharset;

        private FetchedResponse(int statusCode, String contentType, byte[] body,
                                Headers headers, String url, Charset declaredCharset) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
            this.headers = headers;
            this.url = url;
            this.declaredCharset = declaredCharset;
        }

        private String text() {
            Charset charset = declaredCharset != null ? declaredCharset : StandardCharsets.UTF_8;
            return new String(body, charset);
        }
    }

    private static void copyHeaders(FetchResult result, Headers headers) {
        for (String name : headers.names()) {
            result.addHeader(name, String.join(", ", headers.values(name)));
        }
    }

    /**
     * 请求限速：确保同一域名两次请求间隔不小于minInterval
     */
    private void enforceRateLimit(String url) {
        String domainKey = getDomainKey(url);
        Object lock = rateLimitLocks.computeIfAbsent(domainKey, key -> new Object());
        synchronized (lock) {
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
            // Reserve this domain's next request slot before releasing the lock.
            lastFetchTime.put(domainKey, System.currentTimeMillis());
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
    public int getRetryCount() { return retryCount; }
    public long getRetryDelay() { return retryDelay; }
    public int getMaxBodySize() { return maxBodySize; }
    public long getMinInterval() { return minInterval; }
}
