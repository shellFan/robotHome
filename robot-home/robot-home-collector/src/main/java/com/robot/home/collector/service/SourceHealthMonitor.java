package com.robot.home.collector.service;

import com.robot.home.collector.entity.CrawlerSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 采集源健康度监控
 * 状态机：HEALTHY → DEGRADED → FAILED → DISABLED
 * 连续失败自动暂停，恢复后自动降级
 */
@Component
public class SourceHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(SourceHealthMonitor.class);

    /** 健康状态枚举 */
    public enum HealthStatus {
        HEALTHY,    // 健康：成功率 > 80%
        DEGRADED,   // 降级：成功率 50%-80% 或连续失败3-5次
        FAILED,     // 失败：成功率 < 50% 或连续失败6-9次
        DISABLED    // 禁用：连续失败10次以上，需人工介入
    }

    /** 连续失败阈值 */
    private static final int DEGRADED_THRESHOLD = 3;    // 连续3次失败→DEGRADED
    private static final int FAILED_THRESHOLD = 6;      // 连续6次失败→FAILED
    private static final int DISABLED_THRESHOLD = 10;   // 连续10次失败→DISABLED

    /** 恢复阈值：连续成功次数达到此值后状态升级 */
    private static final int RECOVER_THRESHOLD = 3;

    /** 健康度缓存（sourceId → HealthState） */
    private final Map<Long, HealthState> healthCache = new ConcurrentHashMap<>();

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
}