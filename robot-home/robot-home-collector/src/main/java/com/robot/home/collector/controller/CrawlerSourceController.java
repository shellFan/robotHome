package com.robot.home.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.collector.entity.CrawlerSource;
import com.robot.home.collector.mapper.CrawlerSourceMapper;
import com.robot.home.collector.service.DeduplicationService;
import com.robot.home.collector.service.SourceHealthMonitor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源管理API
 */
@RestController
@RequestMapping("/api/crawler/source")
public class CrawlerSourceController {

    private static final Logger log = LoggerFactory.getLogger(CrawlerSourceController.class);

    @Autowired
    private CrawlerSourceMapper sourceMapper;

    @Autowired
    private DeduplicationService deduplicationService;

    @Autowired
    private SourceHealthMonitor sourceHealthMonitor;

    /**
     * 分页查询数据源
     */
    @GetMapping("/list")
    public Page<CrawlerSource> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Integer crawlEnabled) {

        Page<CrawlerSource> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<CrawlerSource> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(sourceType)) {
            wrapper.eq(CrawlerSource::getSourceType, sourceType);
        }
        if (crawlEnabled != null) {
            wrapper.eq(CrawlerSource::getCrawlEnabled, crawlEnabled);
        }
        wrapper.orderByDesc(CrawlerSource::getCreateTime);

        return sourceMapper.selectPage(pageReq, wrapper);
    }

    /**
     * 获取数据源详情
     */
    @GetMapping("/{id}")
    public CrawlerSource get(@PathVariable Long id) {
        return sourceMapper.selectById(id);
    }

    /**
     * 创建数据源
     */
    @PostMapping
    public CrawlerSource create(@RequestBody CrawlerSource source) {
        source.setCreateTime(LocalDateTime.now());
        source.setUpdateTime(LocalDateTime.now());
        if (source.getCrawlEnabled() == null) {
            source.setCrawlEnabled(1);
        }
        if (source.getFetchMode() == null) {
            source.setFetchMode("http");
        }
        if (source.getStatus() == null) {
            source.setStatus(1);
        }
        sourceMapper.insert(source);
        log.info("Created crawler source: id={}, name={}", source.getId(), source.getSourceName());
        return source;
    }

    /**
     * 更新数据源
     */
    @PutMapping("/{id}")
    public CrawlerSource update(@PathVariable Long id, @RequestBody CrawlerSource source) {
        source.setId(id);
        source.setUpdateTime(LocalDateTime.now());
        sourceMapper.updateById(source);
        log.info("Updated crawler source: id={}", id);
        return source;
    }

    /**
     * 删除数据源
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        sourceMapper.deleteById(id);
        log.info("Deleted crawler source: id={}", id);
    }

    /**
     * 启用/禁用数据源
     */
    @PutMapping("/{id}/toggle")
    public CrawlerSource toggle(@PathVariable Long id) {
        CrawlerSource source = sourceMapper.selectById(id);
        if (source != null) {
            source.setCrawlEnabled(source.getCrawlEnabled() == 1 ? 0 : 1);
            source.setUpdateTime(LocalDateTime.now());
            sourceMapper.updateById(source);
        }
        return source;
    }

    /**
     * 获取所有活跃数据源
     */
    @GetMapping("/active")
    public List<CrawlerSource> listActive() {
        return sourceMapper.selectList(
                new LambdaQueryWrapper<CrawlerSource>()
                        .eq(CrawlerSource::getCrawlEnabled, 1)
                        .orderByAsc(CrawlerSource::getSourceName));
    }

    /**
     * 清除去重数据（用于重新抓取已处理过的URL）
     */
    @PostMapping("/clear-dedup")
    public java.util.Map<String, Object> clearDedup() {
        int cleared = deduplicationService.clearAllDedupData();
        log.info("Cleared all dedup data: {} URL records removed", cleared);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "Cleared " + cleared + " dedup records");
        return result;
    }

    // ==================== Phase12: 真实HTTP健康探测 ====================

    /**
     * 探测单个数据源的健康状态（真实HTTP请求）
     * 不模拟，不猜测，只记录真实HTTP结果
     */
    @PostMapping("/{id}/probe")
    public Map<String, Object> probeSource(@PathVariable Long id) {
        CrawlerSource source = sourceMapper.selectById(id);
        if (source == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Source not found: " + id);
            return error;
        }

        log.info("Manual health probe triggered for source: id={}, name={}", id, source.getSourceName());
        SourceHealthMonitor.HealthProbeResult probeResult = sourceHealthMonitor.probeSourceHealth(source);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sourceId", probeResult.getSourceId());
        result.put("sourceName", probeResult.getSourceName());
        result.put("baseUrl", probeResult.getBaseUrl());
        result.put("healthStatus", probeResult.getHealthStatus().name());
        result.put("httpStatus", probeResult.getHttpStatus());
        result.put("dnsResult", probeResult.getDnsResult());
        result.put("dnsLatencyMs", probeResult.getDnsLatencyMs());
        result.put("resolvedIp", probeResult.getResolvedIp());
        result.put("latencyMs", probeResult.getLatencyMs());
        result.put("contentType", probeResult.getContentType());
        result.put("responseSize", probeResult.getResponseSize());
        result.put("finalUrl", probeResult.getFinalUrl());
        result.put("redirectCount", probeResult.getRedirectCount());
        result.put("robotsTxtChecked", probeResult.getRobotsTxtChecked());
        result.put("robotsTxtAllowed", probeResult.getRobotsTxtAllowed());
        result.put("failReason", probeResult.getFailReason());
        result.put("probeTime", probeResult.getProbeTime());
        return result;
    }

    /**
     * 批量探测所有启用数据源的健康状态
     * 限制：一次最多探测10个源，避免过于频繁
     */
    @PostMapping("/probe-all")
    public Map<String, Object> probeAllSources(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<CrawlerSource> activeSources = sourceMapper.selectList(
                new LambdaQueryWrapper<CrawlerSource>()
                        .eq(CrawlerSource::getCrawlEnabled, 1)
                        .eq(CrawlerSource::getStatus, 1)
                        .orderByDesc(CrawlerSource::getPriority)
                        .last("LIMIT " + Math.min(limit, 10)));

        log.info("Batch health probe: probing {} active sources", activeSources.size());

        List<SourceHealthMonitor.HealthProbeResult> results = sourceHealthMonitor.probeSources(activeSources);

        // 统计
        long healthy = results.stream().filter(r -> r.getHealthStatus() == SourceHealthMonitor.HealthStatus.HEALTHY).count();
        long degraded = results.stream().filter(r -> r.getHealthStatus() == SourceHealthMonitor.HealthStatus.DEGRADED).count();
        long failed = results.stream().filter(r -> r.getHealthStatus() == SourceHealthMonitor.HealthStatus.FAILED).count();
        long blocked = results.stream().filter(r -> r.getHealthStatus() == SourceHealthMonitor.HealthStatus.BLOCKED).count();
        long requiresAuth = results.stream().filter(r -> r.getHealthStatus() == SourceHealthMonitor.HealthStatus.REQUIRES_AUTH).count();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("totalProbed", results.size());
        result.put("healthy", healthy);
        result.put("degraded", degraded);
        result.put("failed", failed);
        result.put("blocked", blocked);
        result.put("requiresAuth", requiresAuth);
        result.put("results", results);
        return result;
    }
}