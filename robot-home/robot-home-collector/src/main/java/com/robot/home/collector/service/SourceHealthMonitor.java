package com.robot.home.collector.service;

import com.robot.home.collector.entity.CrawlerSource;
import com.robot.home.collector.fetcher.FetchResult;
import com.robot.home.collector.fetcher.HttpFetcher;
import com.robot.home.collector.mapper.CrawlerSourceMapper;
import com.robot.home.collector.parser.RobotsTxtParser;
import com.robot.home.collector.util.UrlSecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 采集源健康度监控
 * 状态机：HEALTHY → DEGRADED → FAILED → DISABLED
 * 连续失败自动暂停，恢复后自动降级
 * 
 * Phase12: 新增 probeSourceHealth — 真实HTTP探测，不依赖模拟
 */
@Component
public class SourceHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(SourceHealthMonitor.class);

    /** 健康状态枚举 */
    public enum HealthStatus {
        HEALTHY,    // 健康：成功率 > 80%
        DEGRADED,   // 降级：成功率 50%-80% 或连续失败3-5次
        FAILED,     // 失败：成功率 < 50% 或连续失败6-9次
        DISABLED,   // 禁用：连续失败10次以上，需人工介入
        UNKNOWN,    // 未知：尚未探测
        BLOCKED,    // 被封：403/429/Cloudflare/Captcha
        REQUIRES_AUTH  // 需要认证：401/登录墙
    }

    /** 连续失败阈值 */
    private static final int DEGRADED_THRESHOLD = 3;    // 连续3次失败→DEGRADED
    private static final int FAILED_THRESHOLD = 6;      // 连续6次失败→FAILED
    private static final int DISABLED_THRESHOLD = 10;   // 连续10次失败→DISABLED

    /** 恢复阈值：连续成功次数达到此值后状态升级 */
    private static final int RECOVER_THRESHOLD = 3;

    /** HTTP探测超时(ms) */
    private static final int PROBE_TIMEOUT_MS = 15000;

    /** 健康度缓存（sourceId → HealthState） */
    private final Map<Long, HealthState> healthCache = new ConcurrentHashMap<>();

    @Autowired
    private HttpFetcher httpFetcher;

    @Autowired
    private RobotsTxtParser robotsTxtParser;

    @Autowired
    private CrawlerSourceMapper sourceMapper;

    /**
     * 内部健康状态
     */
    private static class HealthState {
        HealthStatus status = HealthStatus.HEALTHY;
        int consecutiveFailures = 0;
        int consecutiveSuccesses = 0;
        long totalCrawls = 0;
        long totalSuccessCrawls = 0;
        long avgLatencyMs = 0;
        LocalDateTime lastSuccessTime;
        LocalDateTime lastFailTime;
        String lastFailReason;
    }

    /**
     * 记录成功抓取
     * @param sourceId 采集源ID
     * @param latencyMs 响应时间(ms)
     */
    public void recordSuccess(long sourceId, long latencyMs) {
        HealthState state = healthCache.computeIfAbsent(sourceId, k -> new HealthState());
        state.consecutiveFailures = 0;
        state.consecutiveSuccesses++;
        state.totalCrawls++;
        state.totalSuccessCrawls++;
        state.lastSuccessTime = LocalDateTime.now();

        // 更新平均延迟（指数移动平均）
        if (state.avgLatencyMs == 0) {
            state.avgLatencyMs = latencyMs;
        } else {
            state.avgLatencyMs = (long) (state.avgLatencyMs * 0.7 + latencyMs * 0.3);
        }

        // 检查状态恢复
        if (state.status != HealthStatus.HEALTHY && state.consecutiveSuccesses >= RECOVER_THRESHOLD) {
            HealthStatus oldStatus = state.status;
            if (state.status == HealthStatus.DISABLED) {
                state.status = HealthStatus.FAILED;
            } else if (state.status == HealthStatus.FAILED) {
                state.status = HealthStatus.DEGRADED;
            } else if (state.status == HealthStatus.DEGRADED) {
                state.status = HealthStatus.HEALTHY;
            }
            state.consecutiveSuccesses = 0; // 重置连续成功计数
            log.info("Source {} health recovered: {} → {} (successes={}, latency={}ms)",
                    sourceId, oldStatus, state.status, state.consecutiveSuccesses, latencyMs);
        }

        log.debug("Source {} success: latency={}ms, avgLatency={}ms, status={}",
                sourceId, latencyMs, state.avgLatencyMs, state.status);
    }

    /**
     * 记录失败抓取
     * @param sourceId 采集源ID
     * @param reason 失败原因
     * @param latencyMs 响应时间(ms)，0表示超时/连接失败
     */
    public void recordFailure(long sourceId, String reason, long latencyMs) {
        HealthState state = healthCache.computeIfAbsent(sourceId, k -> new HealthState());
        state.consecutiveSuccesses = 0;
        state.consecutiveFailures++;
        state.totalCrawls++;
        state.lastFailTime = LocalDateTime.now();
        state.lastFailReason = reason;

        // 更新状态
        HealthStatus oldStatus = state.status;
        if (state.consecutiveFailures >= DISABLED_THRESHOLD) {
            state.status = HealthStatus.DISABLED;
        } else if (state.consecutiveFailures >= FAILED_THRESHOLD) {
            state.status = HealthStatus.FAILED;
        } else if (state.consecutiveFailures >= DEGRADED_THRESHOLD) {
            state.status = HealthStatus.DEGRADED;
        }

        if (oldStatus != state.status) {
            log.warn("Source {} health degraded: {} → {} (consecutiveFailures={}, reason={})",
                    sourceId, oldStatus, state.status, state.consecutiveFailures, reason);
        } else {
            log.debug("Source {} failure: consecutiveFailures={}, reason={}, status={}",
                    sourceId, state.consecutiveFailures, reason, state.status);
        }
    }

    /**
     * 获取采集源健康状态
     */
    public HealthStatus getHealthStatus(long sourceId) {
        HealthState state = healthCache.get(sourceId);
        return state != null ? state.status : HealthStatus.HEALTHY;
    }

    /**
     * 判断采集源是否可以继续抓取
     */
    public boolean canCrawl(long sourceId) {
        HealthStatus status = getHealthStatus(sourceId);
        return status != HealthStatus.DISABLED;
    }

    /**
     * 判断采集源是否健康
     */
    public boolean isHealthy(long sourceId) {
        return getHealthStatus(sourceId) == HealthStatus.HEALTHY;
    }

    /**
     * 判断采集源是否降级
     */
    public boolean isDegraded(long sourceId) {
        return getHealthStatus(sourceId) == HealthStatus.DEGRADED;
    }

    /**
     * 获取连续失败次数
     */
    public int getConsecutiveFailures(long sourceId) {
        HealthState state = healthCache.get(sourceId);
        return state != null ? state.consecutiveFailures : 0;
    }

    /**
     * 获取平均延迟
     */
    public long getAvgLatencyMs(long sourceId) {
        HealthState state = healthCache.get(sourceId);
        return state != null ? state.avgLatencyMs : 0;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate(long sourceId) {
        HealthState state = healthCache.get(sourceId);
        if (state == null || state.totalCrawls == 0) return 1.0;
        return (double) state.totalSuccessCrawls / state.totalCrawls;
    }

    /**
     * 获取健康度摘要
     */
    public HealthSummary getHealthSummary(long sourceId) {
        HealthState state = healthCache.get(sourceId);
        if (state == null) {
            return new HealthSummary(sourceId, HealthStatus.HEALTHY, 0, 0, 0, 1.0, null, null, null);
        }
        double successRate = state.totalCrawls > 0 ?
                (double) state.totalSuccessCrawls / state.totalCrawls : 1.0;
        return new HealthSummary(
                sourceId, state.status, state.consecutiveFailures,
                state.totalCrawls, state.avgLatencyMs, successRate,
                state.lastSuccessTime, state.lastFailTime, state.lastFailReason
        );
    }

    /**
     * 将健康状态同步到CrawlerSource实体（用于持久化）
     */
    public void syncToEntity(CrawlerSource source) {
        if (source == null) return;
        HealthState state = healthCache.get(source.getId());
        if (state != null) {
            source.setHealthStatus(state.status.name());
            source.setConsecutiveFailures(state.consecutiveFailures);
            source.setAvgLatencyMs((int) state.avgLatencyMs);
            source.setLastSuccessTime(state.lastSuccessTime);
            source.setLastFailTime(state.lastFailTime);
            source.setLastFailReason(state.lastFailReason);
            source.setTotalCrawls((int) state.totalCrawls);
            source.setTotalSuccessCrawls((int) state.totalSuccessCrawls);
        }
    }

    /**
     * 从CrawlerSource实体加载健康状态（用于启动恢复）
     */
    public void loadFromEntity(CrawlerSource source) {
        if (source == null) return;
        HealthState state = healthCache.computeIfAbsent(source.getId(), k -> new HealthState());

        if (source.getHealthStatus() != null) {
            try {
                state.status = HealthStatus.valueOf(source.getHealthStatus());
            } catch (IllegalArgumentException ignored) {
                state.status = HealthStatus.HEALTHY;
            }
        }
        state.consecutiveFailures = source.getConsecutiveFailures() != null ? source.getConsecutiveFailures() : 0;
        state.avgLatencyMs = source.getAvgLatencyMs() != null ? source.getAvgLatencyMs() : 0;
        state.lastSuccessTime = source.getLastSuccessTime();
        state.lastFailTime = source.getLastFailTime();
        state.lastFailReason = source.getLastFailReason();
        state.totalCrawls = source.getTotalCrawls() != null ? source.getTotalCrawls() : 0;
        state.totalSuccessCrawls = source.getTotalSuccessCrawls() != null ? source.getTotalSuccessCrawls() : 0;
    }

    /**
     * 手动重置健康状态
     */
    public void reset(long sourceId) {
        healthCache.remove(sourceId);
        log.info("Source {} health status reset", sourceId);
    }

    /**
     * 手动启用已禁用的采集源
     */
    public void enable(long sourceId) {
        HealthState state = healthCache.get(sourceId);
        if (state != null && state.status == HealthStatus.DISABLED) {
            state.status = HealthStatus.DEGRADED;
            state.consecutiveFailures = 0;
            state.consecutiveSuccesses = 0;
            log.info("Source {} manually enabled, status set to DEGRADED", sourceId);
        }
    }

    /**
     * 健康度摘要
     */
    public static class HealthSummary {
        private final long sourceId;
        private final HealthStatus status;
        private final int consecutiveFailures;
        private final long totalCrawls;
        private final long avgLatencyMs;
        private final double successRate;
        private final LocalDateTime lastSuccessTime;
        private final LocalDateTime lastFailTime;
        private final String lastFailReason;

        public HealthSummary(long sourceId, HealthStatus status, int consecutiveFailures,
                             long totalCrawls, long avgLatencyMs, double successRate,
                             LocalDateTime lastSuccessTime, LocalDateTime lastFailTime,
                             String lastFailReason) {
            this.sourceId = sourceId;
            this.status = status;
            this.consecutiveFailures = consecutiveFailures;
            this.totalCrawls = totalCrawls;
            this.avgLatencyMs = avgLatencyMs;
            this.successRate = successRate;
            this.lastSuccessTime = lastSuccessTime;
            this.lastFailTime = lastFailTime;
            this.lastFailReason = lastFailReason;
        }

        public long getSourceId() { return sourceId; }
        public HealthStatus getStatus() { return status; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public long getTotalCrawls() { return totalCrawls; }
        public long getAvgLatencyMs() { return avgLatencyMs; }
        public double getSuccessRate() { return successRate; }
        public LocalDateTime getLastSuccessTime() { return lastSuccessTime; }
        public LocalDateTime getLastFailTime() { return lastFailTime; }
        public String getLastFailReason() { return lastFailReason; }

        @Override
        public String toString() {
            return String.format("HealthSummary{source=%d, status=%s, failures=%d, crawls=%d, latency=%dms, rate=%.1f%%}",
                    sourceId, status, consecutiveFailures, totalCrawls, avgLatencyMs, successRate * 100);
        }
    }

    // ==================== Phase12: 真实HTTP健康探测 ====================

    /**
     * 真实HTTP健康探测 — 对采集源执行真实网络请求
     * 不模拟，不猜测，只记录真实HTTP结果
     *
     * 探测内容：
     * 1. DNS解析
     * 2. HTTP状态码
     * 3. 最终URL（跟踪重定向）
     * 4. 重定向次数
     * 5. Content-Type
     * 6. 响应大小
     * 7. 延迟
     * 8. robots.txt检查
     *
     * 安全规则：
     * - 经过SSRF防护（UrlSecurityUtil）
     * - 经过域名限速（HttpFetcher内置）
     * - 不绕过robots.txt
     * - 不关闭TLS验证
     *
     * @param source 采集源
     * @return 探测结果
     */
    public HealthProbeResult probeSourceHealth(CrawlerSource source) {
        HealthProbeResult result = new HealthProbeResult();
        result.setSourceId(source.getId());
        result.setSourceName(source.getSourceName());
        result.setBaseUrl(source.getBaseUrl());
        result.setProbeTime(LocalDateTime.now());

        String url = source.getBaseUrl();
        if (StringUtils.isBlank(url)) {
            result.setDnsResult("NO_URL");
            result.setHttpStatus(0);
            result.setHealthStatus(HealthStatus.FAILED);
            result.setFailReason("Source has no baseUrl configured");
            recordFailure(source.getId(), "NO_URL", 0);
            persistHealthStatus(source, result);
            return result;
        }

        // SSRF防护校验
        if (!UrlSecurityUtil.isAllowedUrl(url)) {
            String reason = UrlSecurityUtil.getRejectionReason(url);
            result.setDnsResult("SSRF_BLOCKED");
            result.setHttpStatus(0);
            result.setHealthStatus(HealthStatus.FAILED);
            result.setFailReason("SSRF protection blocked: " + reason);
            recordFailure(source.getId(), "SSRF_BLOCKED", 0);
            persistHealthStatus(source, result);
            return result;
        }

        // DNS解析
        long dnsStart = System.currentTimeMillis();
        try {
            String host = new java.net.URL(url).getHost();
            InetAddress[] addresses = InetAddress.getAllByName(host);
            long dnsMs = System.currentTimeMillis() - dnsStart;
            result.setDnsResult("OK");
            result.setDnsLatencyMs(dnsMs);
            result.setResolvedIp(addresses[0].getHostAddress());
            log.info("DNS resolved {} -> {} ({}ms)", host, addresses[0].getHostAddress(), dnsMs);
        } catch (Exception e) {
            long dnsMs = System.currentTimeMillis() - dnsStart;
            result.setDnsResult("FAIL");
            result.setDnsLatencyMs(dnsMs);
            result.setHttpStatus(0);
            result.setHealthStatus(HealthStatus.FAILED);
            result.setFailReason("DNS resolution failed: " + e.getMessage());
            recordFailure(source.getId(), "DNS_FAIL", dnsMs);
            persistHealthStatus(source, result);
            return result;
        }

        // HTTP请求（经过HttpFetcher的SSRF防护+限速+重试）
        long fetchStart = System.currentTimeMillis();
        try {
            FetchResult fetchResult = httpFetcher.fetch(url);
            long fetchMs = System.currentTimeMillis() - fetchStart;
            result.setLatencyMs(fetchMs);

            if (fetchResult.getError() != null && fetchResult.getStatusCode() == 0) {
                // 连接级失败（超时、连接拒绝等）
                result.setHttpStatus(0);
                result.setHealthStatus(HealthStatus.FAILED);
                result.setFailReason("Connection failed: " + fetchResult.getError());
                recordFailure(source.getId(), "CONNECTION_FAIL", fetchMs);
                persistHealthStatus(source, result);
                return result;
            }

            result.setHttpStatus(fetchResult.getStatusCode());
            result.setContentType(fetchResult.getContentType());
            result.setFinalUrl(fetchResult.getRedirectedUrl() != null ? fetchResult.getRedirectedUrl() : url);

            // 计算响应大小
            if (fetchResult.getHtml() != null) {
                result.setResponseSize(fetchResult.getHtml().length());
            } else if (fetchResult.getBody() != null) {
                result.setResponseSize(fetchResult.getBody().length);
            }

            // 计算重定向次数
            if (fetchResult.getRedirectedUrl() != null && !fetchResult.getRedirectedUrl().equals(url)) {
                result.setRedirectCount(1); // Jsoup自动跟踪重定向，最多记录1次
            }

            // 根据HTTP状态码判断健康度
            int status = fetchResult.getStatusCode();
            if (status >= 200 && status < 300) {
                result.setHealthStatus(HealthStatus.HEALTHY);
                recordSuccess(source.getId(), fetchMs);
            } else if (status == 401 || status == 407) {
                result.setHealthStatus(HealthStatus.REQUIRES_AUTH);
                result.setFailReason("HTTP " + status + " - Authentication required");
                recordFailure(source.getId(), "AUTH_REQUIRED", fetchMs);
            } else if (status == 403) {
                result.setHealthStatus(HealthStatus.BLOCKED);
                result.setFailReason("HTTP 403 - Access forbidden (possible anti-bot)");
                recordFailure(source.getId(), "FORBIDDEN", fetchMs);
            } else if (status == 429) {
                result.setHealthStatus(HealthStatus.BLOCKED);
                result.setFailReason("HTTP 429 - Rate limited");
                recordFailure(source.getId(), "RATE_LIMITED", fetchMs);
            } else if (status >= 300 && status < 400) {
                // 重定向但未跟踪成功
                result.setHealthStatus(HealthStatus.DEGRADED);
                result.setFailReason("HTTP " + status + " - Redirect not followed");
                recordFailure(source.getId(), "REDIRECT", fetchMs);
            } else if (status >= 400 && status < 500) {
                result.setHealthStatus(HealthStatus.FAILED);
                result.setFailReason("HTTP " + status + " - Client error");
                recordFailure(source.getId(), "HTTP_" + status, fetchMs);
            } else if (status >= 500) {
                result.setHealthStatus(HealthStatus.DEGRADED);
                result.setFailReason("HTTP " + status + " - Server error (may be temporary)");
                recordFailure(source.getId(), "HTTP_" + status, fetchMs);
            }

        } catch (Exception e) {
            long fetchMs = System.currentTimeMillis() - fetchStart;
            result.setLatencyMs(fetchMs);
            result.setHttpStatus(0);
            result.setHealthStatus(HealthStatus.FAILED);
            result.setFailReason("Fetch exception: " + e.getMessage());
            recordFailure(source.getId(), "EXCEPTION", fetchMs);
        }

        // robots.txt检查（仅当HTTP请求成功时）
        if (result.getHealthStatus() == HealthStatus.HEALTHY && source.getRespectRobots() != null && source.getRespectRobots() == 1) {
            try {
                String robotsUrl = extractRobotsUrl(url);
                FetchResult robotsResult = httpFetcher.fetch(robotsUrl);
                if (robotsResult.isSuccess() && robotsResult.isHtml()) {
                    // robots.txt通常返回text/plain，也检查
                    RobotsTxtParser.RobotsRules rules = robotsTxtParser.parse(
                            extractDomain(url), robotsResult.getHtml());
                    boolean allowed = rules.isAllowed("RobotHomeCollector", "/");
                    result.setRobotsTxtChecked(true);
                    result.setRobotsTxtAllowed(allowed);
                    if (!allowed) {
                        result.setHealthStatus(HealthStatus.BLOCKED);
                        result.setFailReason("robots.txt disallows crawling");
                        log.warn("Source {} robots.txt disallows crawling", source.getSourceName());
                    }
                } else {
                    result.setRobotsTxtChecked(true);
                    result.setRobotsTxtAllowed(true); // 无robots.txt默认允许
                }
            } catch (Exception e) {
                log.warn("Failed to check robots.txt for {}: {}", source.getSourceName(), e.getMessage());
                result.setRobotsTxtChecked(true);
                result.setRobotsTxtAllowed(true); // 检查失败默认允许
            }
        }

        // 持久化健康状态到数据库
        persistHealthStatus(source, result);

        log.info("Source health probe: id={}, name={}, status={}, http={}, latency={}ms, dns={}ms",
                source.getId(), source.getSourceName(), result.getHealthStatus(),
                result.getHttpStatus(), result.getLatencyMs(), result.getDnsLatencyMs());

        return result;
    }

    /**
     * 批量探测多个采集源的健康状态
     * @param sources 采集源列表
     * @return 探测结果列表
     */
    public List<HealthProbeResult> probeSources(List<CrawlerSource> sources) {
        List<HealthProbeResult> results = new ArrayList<>();
        for (CrawlerSource source : sources) {
            try {
                HealthProbeResult result = probeSourceHealth(source);
                results.add(result);
                // 探测间隔：避免过于频繁
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Probe failed for source {}: {}", source.getSourceName(), e.getMessage());
                HealthProbeResult failResult = new HealthProbeResult();
                failResult.setSourceId(source.getId());
                failResult.setSourceName(source.getSourceName());
                failResult.setBaseUrl(source.getBaseUrl());
                failResult.setHealthStatus(HealthStatus.FAILED);
                failResult.setFailReason("Probe exception: " + e.getMessage());
                results.add(failResult);
            }
        }
        return results;
    }

    /**
     * 持久化健康状态到数据库
     */
    private void persistHealthStatus(CrawlerSource source, HealthProbeResult probeResult) {
        try {
            syncToEntity(source);
            source.setHealthStatus(probeResult.getHealthStatus().name());
            if (probeResult.getRobotsTxtChecked() != null && probeResult.getRobotsTxtChecked()) {
                source.setRobotsChecked(1);
                source.setRobotsAllowed(probeResult.getRobotsTxtAllowed() ? 1 : 0);
                source.setRobotsCheckedTime(LocalDateTime.now());
            }
            sourceMapper.updateById(source);
            log.info("Persisted health status for source {}: {}", source.getSourceName(), probeResult.getHealthStatus());
        } catch (Exception e) {
            log.warn("Failed to persist health status for source {}: {}", source.getSourceName(), e.getMessage());
        }
    }

    /**
     * 提取robots.txt URL
     */
    private String extractRobotsUrl(String baseUrl) {
        try {
            java.net.URL url = new java.net.URL(baseUrl);
            return url.getProtocol() + "://" + url.getHost() +
                    (url.getPort() > 0 && url.getPort() != url.getDefaultPort() ? ":" + url.getPort() : "") +
                    "/robots.txt";
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 健康探测结果
     */
    public static class HealthProbeResult {
        private Long sourceId;
        private String sourceName;
        private String baseUrl;
        private LocalDateTime probeTime;

        // DNS结果
        private String dnsResult;       // OK / FAIL / NO_URL / SSRF_BLOCKED
        private long dnsLatencyMs;
        private String resolvedIp;

        // HTTP结果
        private int httpStatus;
        private String finalUrl;
        private int redirectCount;
        private String contentType;
        private int responseSize;
        private long latencyMs;

        // robots.txt
        private Boolean robotsTxtChecked;
        private Boolean robotsTxtAllowed;

        // 健康状态
        private HealthStatus healthStatus;
        private String failReason;

        // Getters and Setters
        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public LocalDateTime getProbeTime() { return probeTime; }
        public void setProbeTime(LocalDateTime probeTime) { this.probeTime = probeTime; }
        public String getDnsResult() { return dnsResult; }
        public void setDnsResult(String dnsResult) { this.dnsResult = dnsResult; }
        public long getDnsLatencyMs() { return dnsLatencyMs; }
        public void setDnsLatencyMs(long dnsLatencyMs) { this.dnsLatencyMs = dnsLatencyMs; }
        public String getResolvedIp() { return resolvedIp; }
        public void setResolvedIp(String resolvedIp) { this.resolvedIp = resolvedIp; }
        public int getHttpStatus() { return httpStatus; }
        public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
        public String getFinalUrl() { return finalUrl; }
        public void setFinalUrl(String finalUrl) { this.finalUrl = finalUrl; }
        public int getRedirectCount() { return redirectCount; }
        public void setRedirectCount(int redirectCount) { this.redirectCount = redirectCount; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public int getResponseSize() { return responseSize; }
        public void setResponseSize(int responseSize) { this.responseSize = responseSize; }
        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
        public Boolean getRobotsTxtChecked() { return robotsTxtChecked; }
        public void setRobotsTxtChecked(Boolean robotsTxtChecked) { this.robotsTxtChecked = robotsTxtChecked; }
        public Boolean getRobotsTxtAllowed() { return robotsTxtAllowed; }
        public void setRobotsTxtAllowed(Boolean robotsTxtAllowed) { this.robotsTxtAllowed = robotsTxtAllowed; }
        public HealthStatus getHealthStatus() { return healthStatus; }
        public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }
        public String getFailReason() { return failReason; }
        public void setFailReason(String failReason) { this.failReason = failReason; }

        @Override
        public String toString() {
            return String.format("ProbeResult{id=%d, name=%s, status=%s, http=%d, latency=%dms, dns=%s(%dms), reason=%s}",
                    sourceId, sourceName, healthStatus, httpStatus, latencyMs, dnsResult, dnsLatencyMs,
                    failReason != null ? failReason : "none");
        }
    }
}