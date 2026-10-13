package com.robot.home.collector.controller;

import com.robot.home.collector.service.CrawlerStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 采集统计API
 * Phase12: 支撑真实数据成绩报告
 * 通过 /api/admin/crawler/stats 代理访问（AdminInterceptor + CrawlerProxyController）
 */
@RestController
@RequestMapping("/api/crawler/stats")
public class CrawlerStatsController {

    @Autowired
    private CrawlerStatsService statsService;

    /**
     * 全量采集统计
     * 返回: sources/tasks/urls/articles/products/media/errors 各维度统计
     */
    @GetMapping
    public Map<String, Object> fullStats() {
        return statsService.getFullStats();
    }

    /**
     * 数据源统计
     */
    @GetMapping("/sources")
    public Map<String, Object> sourceStats() {
        return statsService.getSourceStats();
    }

    /**
     * 任务统计
     */
    @GetMapping("/tasks")
    public Map<String, Object> taskStats() {
        return statsService.getTaskStats();
    }

    /**
     * 文章统计
     */
    @GetMapping("/articles")
    public Map<String, Object> articleStats() {
        return statsService.getArticleStats();
    }

    /**
     * 产品统计
     */
    @GetMapping("/products")
    public Map<String, Object> productStats() {
        return statsService.getProductStats();
    }

    /**
     * 媒体统计
     */
    @GetMapping("/media")
    public Map<String, Object> mediaStats() {
        return statsService.getMediaStats();
    }

    /**
     * 错误统计
     */
    @GetMapping("/errors")
    public Map<String, Object> errorStats() {
        return statsService.getErrorStats();
    }
}