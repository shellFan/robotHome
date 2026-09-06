package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robot.home.collector.entity.CrawlerArticle;
import com.robot.home.collector.entity.CrawlerProduct;
import com.robot.home.collector.mapper.CrawlerArticleMapper;
import com.robot.home.collector.mapper.CrawlerProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 发布服务：将采集数据同步到主站 article / robot 及关联表
 * <p>
 * 发布规则：
 * - 仅发布 AUTO_APPROVED 状态的内容（PENDING_REVIEW 需人工审核后才能发布）
 * - 未审核、审核拒绝和失败内容不得写入 article、robot
 * <p>
 * 幂等保障：通过乐观锁（CAS更新 synced 0→1）确保同一条记录不会被重复发布
 * 事务保障：每条记录独立事务（REQUIRES_NEW），单条失败不影响其他记录
 * 重复发布：对已有 articleId/robotIdSynced 的记录执行更新而非重复插入
 * <p>
 * ⚠️ 事务方法已拆分到 PublishTransactionService（独立 Bean），
 * 禁止在本类中自调用 @Transactional 方法，必须通过 Spring 代理调用。
 */
@Service
public class PublishService {

    private static final Logger log = LoggerFactory.getLogger(PublishService.class);

    /** 默认文章分类ID，可通过 admin.default-article-category-id 配置 */
    @Value("${admin.default-article-category-id:1}")
    private Long defaultArticleCategoryId;

    /** PUBLISHING 状态超时恢复阈值（分钟） */
    @Value("${publish.timeout-minutes:30}")
    private int publishTimeoutMinutes;

    @Autowired
    private CrawlerArticleMapper crawlerArticleMapper;

    @Autowired
    private CrawlerProductMapper crawlerProductMapper;

    /** 事务代理 Bean，确保 REQUIRES_NEW 生效 */
    @Autowired
    private PublishTransactionService txService;

    /**
     * 发布待同步的采集文章到主站
     * 仅发布 AUTO_APPROVED 状态的文章
     *
     * @return 成功发布的数量
     */
    public int publishArticles() {
        // 先恢复超时的 PUBLISHING 状态
        txService.recoverTimedOutPublishing(publishTimeoutMinutes);

        List<CrawlerArticle> articles = crawlerArticleMapper.selectList(
                new LambdaQueryWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getMatchStatus, "MATCHED")
                        .eq(CrawlerArticle::getSynced, 0)
                        .and(w -> w
                                .eq(CrawlerArticle::getArticleStatus, "AUTO_APPROVED")
                                .or(o -> o
                                        .eq(CrawlerArticle::getArticleStatus, "FAILED")
                                        .and(i -> i.isNull(CrawlerArticle::getNextRetryTime)
                                                .or().le(CrawlerArticle::getNextRetryTime, LocalDateTime.now()))
                                )
                        )
        );

        if (articles.isEmpty()) {
            log.info("No articles to publish");
            return 0;
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (CrawlerArticle ca : articles) {
            try {
                // 通过 Spring 代理调用，确保 REQUIRES_NEW 事务生效
                boolean published = txService.publishSingleArticle(ca, defaultArticleCategoryId);
                if (published) {
                    successCount++;
                } else {
                    log.info("Article already published by another thread: crawlerId={}", ca.getId());
                }
            } catch (Exception e) {
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.error("Failed to publish article: crawlerId={}, title={}", ca.getId(), ca.getTitle(), e);
                errors.add(String.format("文章[%d]%s: %s", ca.getId(), ca.getTitle(), reason));
                // 通过 Spring 代理标记失败，重置 synced 允许重试
                txService.markArticleFailed(ca.getId(), reason);
            }
        }

        log.info("Article publish complete: total={}, success={}, failed={}", articles.size(), successCount, errors.size());
        if (!errors.isEmpty()) {
            log.warn("Article publish errors:\n{}", String.join("\n", errors));
        }
        return successCount;
    }

    /**
     * 发布待同步的采集产品到主站（含关联表）
     * 仅发布 AUTO_APPROVED 状态的产品
     *
     * @return 成功发布的数量
     */
    public int publishProducts() {
        // 先恢复超时的 PUBLISHING 状态
        txService.recoverTimedOutPublishing(publishTimeoutMinutes);

        List<CrawlerProduct> products = crawlerProductMapper.selectList(
                new LambdaQueryWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getMatchStatus, "MATCHED")
                        .eq(CrawlerProduct::getSynced, 0)
                        .and(w -> w
                                .eq(CrawlerProduct::getProductStatus, "AUTO_APPROVED")
                                .or(o -> o
                                        .eq(CrawlerProduct::getProductStatus, "FAILED")
                                        .and(i -> i.isNull(CrawlerProduct::getNextRetryTime)
                                                .or().le(CrawlerProduct::getNextRetryTime, LocalDateTime.now()))
                                )
                        )
        );

        if (products.isEmpty()) {
            log.info("No products to publish");
            return 0;
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (CrawlerProduct cp : products) {
            try {
                // 通过 Spring 代理调用，确保 REQUIRES_NEW 事务生效
                boolean published = txService.publishSingleProduct(cp, defaultArticleCategoryId);
                if (published) {
                    successCount++;
                } else {
                    log.info("Product already published by another thread: crawlerId={}", cp.getId());
                }
            } catch (Exception e) {
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.error("Failed to publish product: crawlerId={}, name={}", cp.getId(), cp.getProductName(), e);
                errors.add(String.format("产品[%d]%s: %s", cp.getId(), cp.getProductName(), reason));
                // 通过 Spring 代理标记失败，重置 synced 允许重试
                txService.markProductFailed(cp.getId(), reason);
            }
        }

        log.info("Product publish complete: total={}, success={}, failed={}", products.size(), successCount, errors.size());
        if (!errors.isEmpty()) {
            log.warn("Product publish errors:\n{}", String.join("\n", errors));
        }
        return successCount;
    }

    /**
     * 获取待发布的文章数量（AUTO_APPROVED + FAILED 可重试，未到重试时间的不计入）
     */
    public long getPendingArticleCount() {
        return crawlerArticleMapper.selectCount(
                new LambdaQueryWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getMatchStatus, "MATCHED")
                        .eq(CrawlerArticle::getSynced, 0)
                        .and(w -> w
                                .eq(CrawlerArticle::getArticleStatus, "AUTO_APPROVED")
                                .or(o -> o
                                        .eq(CrawlerArticle::getArticleStatus, "FAILED")
                                        .and(i -> i.isNull(CrawlerArticle::getNextRetryTime)
                                                .or().le(CrawlerArticle::getNextRetryTime, LocalDateTime.now()))
                                )
                        )
        );
    }

    /**
     * 获取待发布的产品数量（AUTO_APPROVED + FAILED 可重试，未到重试时间的不计入）
     */
    public long getPendingProductCount() {
        return crawlerProductMapper.selectCount(
                new LambdaQueryWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getMatchStatus, "MATCHED")
                        .eq(CrawlerProduct::getSynced, 0)
                        .and(w -> w
                                .eq(CrawlerProduct::getProductStatus, "AUTO_APPROVED")
                                .or(o -> o
                                        .eq(CrawlerProduct::getProductStatus, "FAILED")
                                        .and(i -> i.isNull(CrawlerProduct::getNextRetryTime)
                                                .or().le(CrawlerProduct::getNextRetryTime, LocalDateTime.now()))
                                )
                        )
        );
    }
}