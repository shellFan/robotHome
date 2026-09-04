package com.robot.home.collector.controller;

import com.robot.home.collector.service.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 发布管理API：将采集数据同步到主站
 */
@RestController
@RequestMapping("/api/crawler/publish")
public class PublishController {

    private static final Logger log = LoggerFactory.getLogger(PublishController.class);

    @Autowired
    private PublishService publishService;

    /**
     * 查看待发布统计
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("pendingArticles", publishService.getPendingArticleCount());
        result.put("pendingProducts", publishService.getPendingProductCount());
        return result;
    }

    /**
     * 发布文章到主站
     */
    @PostMapping("/articles")
    public Map<String, Object> publishArticles() {
        log.info("Manual trigger: publish articles to main site");
        int count = publishService.publishArticles();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("publishedCount", count);
        result.put("message", "Published " + count + " articles");
        return result;
    }

    /**
     * 发布产品到主站
     */
    @PostMapping("/products")
    public Map<String, Object> publishProducts() {
        log.info("Manual trigger: publish products to main site");
        int count = publishService.publishProducts();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("publishedCount", count);
        result.put("message", "Published " + count + " products");
        return result;
    }

    /**
     * 一键发布所有待同步数据
     */
    @PostMapping("/all")
    public Map<String, Object> publishAll() {
        log.info("Manual trigger: publish all to main site");
        int articleCount = publishService.publishArticles();
        int productCount = publishService.publishProducts();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("publishedArticles", articleCount);
        result.put("publishedProducts", productCount);
        result.put("message", "Published " + articleCount + " articles, " + productCount + " products");
        return result;
    }
}