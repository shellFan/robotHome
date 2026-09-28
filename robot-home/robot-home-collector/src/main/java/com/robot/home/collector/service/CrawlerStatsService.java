package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robot.home.collector.entity.*;
import com.robot.home.collector.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集统计服务：提供全量采集数据统计
 * Phase12: 支撑真实数据成绩报告
 */
@Service
public class CrawlerStatsService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerStatsService.class);

    @Autowired
    private CrawlerSourceMapper sourceMapper;
    @Autowired
    private CrawlerTaskMapper taskMapper;
    @Autowired
    private CrawlerUrlMapper urlMapper;
    @Autowired
    private CrawlerPageMapper pageMapper;
    @Autowired
    private CrawlerArticleMapper articleMapper;
    @Autowired
    private CrawlerProductMapper productMapper;
    @Autowired
    private CrawlerMediaMapper mediaMapper;
    @Autowired
    private CrawlerErrorMapper errorMapper;

    /**
     * 获取全量采集统计
     */
    public Map<String, Object> getFullStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("sources", getSourceStats());
        stats.put("tasks", getTaskStats());
        stats.put("urls", getUrlStats());
        stats.put("articles", getArticleStats());
        stats.put("products", getProductStats());
        stats.put("media", getMediaStats());
        stats.put("errors", getErrorStats());
        return stats;
    }

    /**
     * 数据源统计
     */
    public Map<String, Object> getSourceStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = sourceMapper.selectCount(new LambdaQueryWrapper<>());
        long enabled = sourceMapper.selectCount(new LambdaQueryWrapper<CrawlerSource>()
                .eq(CrawlerSource::getCrawlEnabled, 1));
        long disabled = total - enabled;

        result.put("total", total);
        result.put("enabled", enabled);
        result.put("disabled", disabled);

        // 按健康状态分组
        Map<String, Long> byHealth = new LinkedHashMap<>();
        for (String status : new String[]{"HEALTHY", "DEGRADED", "FAILED", "DISABLED", "UNKNOWN"}) {
            long count = sourceMapper.selectCount(new LambdaQueryWrapper<CrawlerSource>()
                    .eq(CrawlerSource::getHealthStatus, status));
            byHealth.put(status, count);
        }
        // 未设置healthStatus的(旧数据)
        long nullHealth = sourceMapper.selectCount(new LambdaQueryWrapper<CrawlerSource>()
                .isNull(CrawlerSource::getHealthStatus));
        if (nullHealth > 0) {
            byHealth.put("UNSET", nullHealth);
        }
        result.put("byHealthStatus", byHealth);

        // 按类型分组
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : new String[]{"WEBSITE", "RSS", "SITEMAP", "WECHAT", "MANUAL"}) {
            long count = sourceMapper.selectCount(new LambdaQueryWrapper<CrawlerSource>()
                    .eq(CrawlerSource::getSourceType, type));
            if (count > 0) {
                byType.put(type, count);
            }
        }
        result.put("byType", byType);

        // 按地区分组
        Map<String, Long> byRegion = new LinkedHashMap<>();
        for (String region : new String[]{"CN", "US", "JP", "EU", "CA", "DE", "NO"}) {
            long count = sourceMapper.selectCount(new LambdaQueryWrapper<CrawlerSource>()
                    .eq(CrawlerSource::getRegion, region));
            if (count > 0) {
                byRegion.put(region, count);
            }
        }
        result.put("byRegion", byRegion);

        return result;
    }

    /**
     * 任务统计
     */
    public Map<String, Object> getTaskStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = taskMapper.selectCount(new LambdaQueryWrapper<>());
        result.put("total", total);

        // 按状态分组
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : new String[]{"QUEUED", "RUNNING", "COMPLETED", "FAILED", "STOPPED", "PENDING"}) {
            long count = taskMapper.selectCount(new LambdaQueryWrapper<CrawlerTask>()
                    .eq(CrawlerTask::getStatus, status));
            if (count > 0) {
                byStatus.put(status, count);
            }
        }
        result.put("byStatus", byStatus);

        // 汇总统计
        long urlsDiscovered = taskMapper.selectList(new LambdaQueryWrapper<CrawlerTask>()
                .select(CrawlerTask::getUrlsDiscovered)).stream()
                .mapToLong(t -> t.getUrlsDiscovered() != null ? t.getUrlsDiscovered() : 0).sum();
        long urlsSuccess = taskMapper.selectList(new LambdaQueryWrapper<CrawlerTask>()
                .select(CrawlerTask::getUrlsSuccess)).stream()
                .mapToLong(t -> t.getUrlsSuccess() != null ? t.getUrlsSuccess() : 0).sum();
        long urlsFailed = taskMapper.selectList(new LambdaQueryWrapper<CrawlerTask>()
                .select(CrawlerTask::getUrlsFailed)).stream()
                .mapToLong(t -> t.getUrlsFailed() != null ? t.getUrlsFailed() : 0).sum();
        long articlesNew = taskMapper.selectList(new LambdaQueryWrapper<CrawlerTask>()
                .select(CrawlerTask::getArticlesNew)).stream()
                .mapToLong(t -> t.getArticlesNew() != null ? t.getArticlesNew() : 0).sum();
        long productsNew = taskMapper.selectList(new LambdaQueryWrapper<CrawlerTask>()
                .select(CrawlerTask::getProductsNew)).stream()
                .mapToLong(t -> t.getProductsNew() != null ? t.getProductsNew() : 0).sum();

        result.put("totalUrlsDiscovered", urlsDiscovered);
        result.put("totalUrlsSuccess", urlsSuccess);
        result.put("totalUrlsFailed", urlsFailed);
        result.put("totalArticlesNew", articlesNew);
        result.put("totalProductsNew", productsNew);

        return result;
    }

    /**
     * URL统计
     */
    public Map<String, Object> getUrlStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = urlMapper.selectCount(new LambdaQueryWrapper<>());
        result.put("total", total);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : new String[]{"DISCOVERED", "QUEUED", "FETCHING", "SUCCESS", "FAILED", "SKIPPED", "BLOCKED"}) {
            long count = urlMapper.selectCount(new LambdaQueryWrapper<CrawlerUrl>()
                    .eq(CrawlerUrl::getUrlStatus, status));
            if (count > 0) {
                byStatus.put(status, count);
            }
        }
        result.put("byStatus", byStatus);
        return result;
    }

    /**
     * 文章统计
     */
    public Map<String, Object> getArticleStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = articleMapper.selectCount(new LambdaQueryWrapper<>());
        result.put("total", total);

        // 按article_status分组
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : new String[]{"CRAWLED", "PARSED", "PENDING_REVIEW", "AUTO_APPROVED", "PUBLISHED", "REJECTED", "DUPLICATE", "FAILED"}) {
            long count = articleMapper.selectCount(new LambdaQueryWrapper<CrawlerArticle>()
                    .eq(CrawlerArticle::getArticleStatus, status));
            if (count > 0) {
                byStatus.put(status, count);
            }
        }
        result.put("byArticleStatus", byStatus);

        // 按match_status分组
        Map<String, Long> byMatch = new LinkedHashMap<>();
        for (String status : new String[]{"PENDING", "MATCHED", "UNMATCHED"}) {
            long count = articleMapper.selectCount(new LambdaQueryWrapper<CrawlerArticle>()
                    .eq(CrawlerArticle::getMatchStatus, status));
            if (count > 0) {
                byMatch.put(status, count);
            }
        }
        result.put("byMatchStatus", byMatch);

        // 已同步/未同步
        long synced = articleMapper.selectCount(new LambdaQueryWrapper<CrawlerArticle>()
                .eq(CrawlerArticle::getSynced, 1));
        result.put("synced", synced);
        result.put("unsynced", total - synced);

        return result;
    }

    /**
     * 产品统计
     */
    public Map<String, Object> getProductStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = productMapper.selectCount(new LambdaQueryWrapper<>());
        result.put("total", total);

        // 按product_status分组
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : new String[]{"CRAWLED", "PARSED", "PENDING_REVIEW", "AUTO_APPROVED", "PUBLISHED", "REJECTED", "DUPLICATE", "FAILED"}) {
            long count = productMapper.selectCount(new LambdaQueryWrapper<CrawlerProduct>()
                    .eq(CrawlerProduct::getProductStatus, status));
            if (count > 0) {
                byStatus.put(status, count);
            }
        }
        result.put("byProductStatus", byStatus);

        // 按match_status分组
        Map<String, Long> byMatch = new LinkedHashMap<>();
        for (String status : new String[]{"PENDING", "MATCHED", "UNMATCHED"}) {
            long count = productMapper.selectCount(new LambdaQueryWrapper<CrawlerProduct>()
                    .eq(CrawlerProduct::getMatchStatus, status));
            if (count > 0) {
                byMatch.put(status, count);
            }
        }
        result.put("byMatchStatus", byMatch);

        // 已同步/未同步
        long synced = productMapper.selectCount(new LambdaQueryWrapper<CrawlerProduct>()
                .eq(CrawlerProduct::getSynced, 1));
        result.put("synced", synced);
        result.put("unsynced", total - synced);

        // 品牌匹配统计
        long withBrandId = productMapper.selectCount(new LambdaQueryWrapper<CrawlerProduct>()
                .isNotNull(CrawlerProduct::getBrandId).ne(CrawlerProduct::getBrandId, 0));
        long withRobotId = productMapper.selectCount(new LambdaQueryWrapper<CrawlerProduct>()
                .isNotNull(CrawlerProduct::getRobotId).ne(CrawlerProduct::getRobotId, 0));
        result.put("withBrandMatch", withBrandId);
        result.put("withRobotMatch", withRobotId);

        return result;
    }

    /**
     * 媒体统计
     */
    public Map<String, Object> getMediaStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = mediaMapper.selectCount(new LambdaQueryWrapper<>());
        result.put("total", total);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : new String[]{"PENDING", "SUCCESS", "FAILED"}) {
            long count = mediaMapper.selectCount(new LambdaQueryWrapper<CrawlerMedia>()
                    .eq(CrawlerMedia::getDownloadStatus, status));
            if (count > 0) {
                byStatus.put(status, count);
            }
        }
        result.put("byDownloadStatus", byStatus);
        return result;
    }

    /**
     * 错误统计
     */
    public Map<String, Object> getErrorStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = errorMapper.selectCount(new LambdaQueryWrapper<>());
        result.put("total", total);

        long unresolved = errorMapper.selectCount(new LambdaQueryWrapper<CrawlerError>()
                .eq(CrawlerError::getResolved, 0));
        result.put("unresolved", unresolved);
        result.put("resolved", total - unresolved);

        // 按错误类型分组
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : new String[]{"NETWORK", "TIMEOUT", "HTTP_403", "HTTP_404", "PARSE_ERROR",
                "JS_RENDER_ERROR", "DUPLICATE", "DATABASE", "IMAGE", "BLOCKED", "UNKNOWN"}) {
            long count = errorMapper.selectCount(new LambdaQueryWrapper<CrawlerError>()
                    .eq(CrawlerError::getErrorType, type));
            if (count > 0) {
                byType.put(type, count);
            }
        }
        result.put("byErrorType", byType);

        return result;
    }
}