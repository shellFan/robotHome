package com.robot.home.collector.fetcher;

import java.util.HashMap;
import java.util.Map;

/**
 * 抓取结果
 */
public class FetchResult {

    private String url;
    private int statusCode;
    private String html;
    private byte[] body;
    private String contentType;
    private String encoding;
    private Map<String, String> headers = new HashMap<>();
    private long fetchTimeMs;
    private boolean fromCache;
    private String error;
    private String redirectedUrl;

    public FetchResult() {
    }

    public FetchResult(String url, int statusCode, String html) {
        this.url = url;
        this.statusCode = statusCode;
        this.html = html;
    }

    public static FetchResult success(String url, int statusCode, String html) {
        return new FetchResult(url, statusCode, html);
    }

    public static FetchResult fail(String url, String error) {
        FetchResult result = new FetchResult();
        result.setUrl(url);
        result.setError(error);
        result.setStatusCode(0);
        return result;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300 && (html != null || body != null);
    }

    public boolean isHtml() {
        if (contentType != null) {
            return contentType.contains("text/html") || contentType.contains("application/xhtml");
        }
        return html != null && html.trim().startsWith("<");
    }

    public boolean isXml() {
        if (contentType != null) {
            return contentType.contains("xml");
        }
        return html != null && html.trim().startsWith("<?xml");
    }

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }
    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public long getFetchTimeMs() { return fetchTimeMs; }
    public void setFetchTimeMs(long fetchTimeMs) { this.fetchTimeMs = fetchTimeMs; }
    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getRedirectedUrl() { return redirectedUrl; }
    public void setRedirectedUrl(String redirectedUrl) { this.redirectedUrl = redirectedUrl; }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    @Override
    public String toString() {
        return "FetchResult{url='" + url + "', status=" + statusCode +
                ", htmlLen=" + (html != null ? html.length() : 0) +
                ", time=" + fetchTimeMs + "ms" +
                (error != null ? ", error='" + error + "'" : "") + "}";
    }
}