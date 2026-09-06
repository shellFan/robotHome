package com.robot.home.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.collector.entity.CrawlerArticle;
import com.robot.home.collector.entity.CrawlerProduct;
import com.robot.home.collector.mapper.CrawlerArticleMapper;
import com.robot.home.collector.mapper.CrawlerProductMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 内容审核管理API
 * 管理采集到的文章和产品，支持审核、发布、拒绝
 */
@RestController
@RequestMapping("/api/crawler/content")
public class CrawlerContentController {

    private static final Logger log = LoggerFactory.getLogger(CrawlerContentController.class);

    @Autowired
    private CrawlerArticleMapper articleMapper;

    @Autowired
    private CrawlerProductMapper productMapper;

    // ==================== 文章管理 ====================

    /**
     * 分页查询文章
     */
    @GetMapping("/article/list")
    public Page<CrawlerArticle> listArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String articleStatus,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long sourceId) {

        Page<CrawlerArticle> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<CrawlerArticle> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(articleStatus)) {
            wrapper.eq(CrawlerArticle::getArticleStatus, articleStatus);
        }
        if (brandId != null) {
            wrapper.eq(CrawlerArticle::getBrandId, brandId);
        }
        if (sourceId != null) {
            wrapper.eq(CrawlerArticle::getSourceId, sourceId);
        }
        wrapper.orderByDesc(CrawlerArticle::getCreateTime);

        return articleMapper.selectPage(pageReq, wrapper);
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/article/{id}")
    public CrawlerArticle getArticle(@PathVariable Long id) {
        CrawlerArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在: id=" + id);
        }
        return article;
    }

    /**
     * 审核文章（通过/拒绝）
     */
    @PutMapping("/article/{id}/review")
    public CrawlerArticle reviewArticle(
            @PathVariable Long id,
            @RequestParam String action, // approve, reject
            @RequestParam(required = false) String reason) {

        CrawlerArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在: id=" + id);
        }

        String status;
        switch (action) {
            case "approve":
                status = "AUTO_APPROVED";
                break;
            case "reject":
                status = "REJECTED";
                break;
            default:
                throw new IllegalArgumentException("不支持的审核操作: " + action);
        }
        article.setArticleStatus(status);

        article.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(article);
        log.info("Reviewed article: id={}, action={}", id, action);
        return article;
    }

    /**
     * 批量审核文章
     */
    @PutMapping("/article/batch-review")
    public Map<String, Object> batchReviewArticles(
            @RequestBody java.util.List<Long> ids,
            @RequestParam String action) {

        int count = 0;
        String status = "approve".equals(action) ? "AUTO_APPROVED" : "REJECTED";

        for (Long id : ids) {
            CrawlerArticle article = articleMapper.selectById(id);
            if (article != null && "PENDING_REVIEW".equals(article.getArticleStatus())) {
                article.setArticleStatus(status);
                article.setUpdateTime(LocalDateTime.now());
                articleMapper.updateById(article);
                count++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("updated", count);
        return result;
    }

    // ==================== 产品管理 ====================

    /**
     * 分页查询产品
     */
    @GetMapping("/product/list")
    public Page<CrawlerProduct> listProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String productStatus,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long sourceId) {

        Page<CrawlerProduct> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<CrawlerProduct> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(productStatus)) {
            wrapper.eq(CrawlerProduct::getProductStatus, productStatus);
        }
        if (brandId != null) {
            wrapper.eq(CrawlerProduct::getBrandId, brandId);
        }
        if (sourceId != null) {
            wrapper.eq(CrawlerProduct::getSourceId, sourceId);
        }
        wrapper.orderByDesc(CrawlerProduct::getCreateTime);

        return productMapper.selectPage(pageReq, wrapper);
    }

    /**
     * 获取产品详情
     */
    @GetMapping("/product/{id}")
    public CrawlerProduct getProduct(@PathVariable Long id) {
        CrawlerProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("产品不存在: id=" + id);
        }
        return product;
    }

    /**
     * 审核产品
     */
    @PutMapping("/product/{id}/review")
    public CrawlerProduct reviewProduct(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(required = false) String reason) {

        CrawlerProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("产品不存在: id=" + id);
        }

        String status;
        switch (action) {
            case "approve":
                status = "AUTO_APPROVED";
                break;
            case "reject":
                status = "REJECTED";
                break;
            default:
                throw new IllegalArgumentException("不支持的审核操作: " + action);
        }
        product.setProductStatus(status);

        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        log.info("Reviewed product: id={}, action={}", id, action);
        return product;
    }

    /**
     * 批量审核产品
     */
    @PutMapping("/product/batch-review")
    public Map<String, Object> batchReviewProducts(
            @RequestBody java.util.List<Long> ids,
            @RequestParam String action) {

        int count = 0;
        String status = "approve".equals(action) ? "AUTO_APPROVED" : "REJECTED";

        for (Long id : ids) {
            CrawlerProduct product = productMapper.selectById(id);
            if (product != null && "PENDING_REVIEW".equals(product.getProductStatus())) {
                product.setProductStatus(status);
                product.setUpdateTime(LocalDateTime.now());
                productMapper.updateById(product);
                count++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("updated", count);
        return result;
    }

    // ==================== 统计 ====================

    /**
     * 内容统计
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();

        // 文章统计
        long articleTotal = articleMapper.selectCount(new LambdaQueryWrapper<>());
        long articlePending = articleMapper.selectCount(
                new LambdaQueryWrapper<CrawlerArticle>().eq(CrawlerArticle::getArticleStatus, "PENDING_REVIEW"));
        long articleApproved = articleMapper.selectCount(
                new LambdaQueryWrapper<CrawlerArticle>().eq(CrawlerArticle::getArticleStatus, "AUTO_APPROVED"));
        long articleRejected = articleMapper.selectCount(
                new LambdaQueryWrapper<CrawlerArticle>().eq(CrawlerArticle::getArticleStatus, "REJECTED"));

        // 产品统计
        long productTotal = productMapper.selectCount(new LambdaQueryWrapper<>());
        long productPending = productMapper.selectCount(
                new LambdaQueryWrapper<CrawlerProduct>().eq(CrawlerProduct::getProductStatus, "PENDING_REVIEW"));
        long productApproved = productMapper.selectCount(
                new LambdaQueryWrapper<CrawlerProduct>().eq(CrawlerProduct::getProductStatus, "AUTO_APPROVED"));
        long productRejected = productMapper.selectCount(
                new LambdaQueryWrapper<CrawlerProduct>().eq(CrawlerProduct::getProductStatus, "REJECTED"));

        stats.put("articleTotal", articleTotal);
        stats.put("articlePending", articlePending);
        stats.put("articleApproved", articleApproved);
        stats.put("articleRejected", articleRejected);
        stats.put("productTotal", productTotal);
        stats.put("productPending", productPending);
        stats.put("productApproved", productApproved);
        stats.put("productRejected", productRejected);

        return stats;
    }
}