package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robot.home.collector.entity.*;
import com.robot.home.collector.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 发布事务服务：独立 Bean，确保 @Transactional(REQUIRES_NEW) 通过 Spring 代理生效。
 * <p>
 * 设计原则：
 * - 每条采集记录在独立事务中发布，单条失败不影响其他记录
 * - 主表 + 全部关联表在同一事务中原子提交
 * - 数据库写入异常必须抛出，触发事务回滚
 * - 数据格式解析错误提前校验，转入 PENDING_REVIEW 而非静默丢弃
 * <p>
 * ⚠️ 禁止在 PublishService 中通过 self-invocation 调用此类方法，
 * 必须通过 Spring 注入的代理调用，否则事务不生效。
 */
@Service
public class PublishTransactionService {

    private static final Logger log = LoggerFactory.getLogger(PublishTransactionService.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    /** 最大重试次数，超过后标记为 MANUAL_REVIEW 需人工介入 */
    @Value("${publish.max-retries:3}")
    private int maxRetryCount;

    /** 重试退避基础间隔（秒），实际间隔 = baseInterval * 2^(retryCount-1) */
    @Value("${publish.retry-base-interval-seconds:60}")
    private int retryBaseIntervalSeconds;

    /** 发布结果：区分关键警告（需人工审核）和非关键警告（仅记录） */
    private static class PublishResult {
        final List<String> warnings = new ArrayList<>();
        boolean needsReview = false;
    }

    @Autowired
    private CrawlerArticleMapper crawlerArticleMapper;

    @Autowired
    private CrawlerProductMapper crawlerProductMapper;

    @Autowired
    private CrawlerMediaMapper crawlerMediaMapper;

    @Autowired
    private PublishArticleMapper publishArticleMapper;

    @Autowired
    private PublishRobotMapper publishRobotMapper;

    @Autowired
    private PublishRobotImageMapper publishRobotImageMapper;

    @Autowired
    private PublishRobotPriceMapper publishRobotPriceMapper;

    @Autowired
    private PublishRobotTagMapper publishRobotTagMapper;

    @Autowired
    private PublishRobotVideoMapper publishRobotVideoMapper;

    @Autowired
    private PublishRobotParamValueMapper publishRobotParamValueMapper;

    @Autowired
    private PublishRobotParamDefMapper publishRobotParamDefMapper;

    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;

    /**
     * 发布单篇文章（独立事务 + 乐观锁幂等）
     * 重复发布时更新已有文章而非重复插入
     *
     * @return true=发布成功, false=CAS失败(已被其他线程发布)
     * @throws RuntimeException 数据库写入失败时抛出，触发事务回滚
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean publishSingleArticle(CrawlerArticle ca, Long defaultCategoryId) {
        // 乐观锁：CAS更新 synced 0→1
        int affected = crawlerArticleMapper.update(null,
                new LambdaUpdateWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getId, ca.getId())
                        .eq(CrawlerArticle::getSynced, 0)
                        .set(CrawlerArticle::getSynced, 1)
                        .set(CrawlerArticle::getArticleStatus, "PUBLISHING")
                        .set(CrawlerArticle::getUpdateTime, LocalDateTime.now())
        );
        if (affected == 0) {
            return false;
        }

        try {
            // 检查是否已有发布记录（重复发布场景）
            if (ca.getArticleId() != null && ca.getArticleId() > 0) {
                PublishArticle existing = publishArticleMapper.selectById(ca.getArticleId());
                if (existing != null) {
                    updateArticleFromCrawler(existing, ca, defaultCategoryId);
                    publishArticleMapper.updateById(existing);
                    markArticlePublished(ca.getId(), existing.getId());
                    log.info("Updated existing article: crawlerId={}, articleId={}", ca.getId(), existing.getId());
                    return true;
                }
            }

            // 新建文章
            PublishArticle article = convertToArticle(ca, defaultCategoryId);
            publishArticleMapper.insert(article);
            markArticlePublished(ca.getId(), article.getId());
            log.info("Published article: crawlerId={}, articleId={}, title={}", ca.getId(), article.getId(), ca.getTitle());
            return true;
        } catch (Exception e) {
            // 任何异常都回滚事务，由调用方标记 FAILED
            throw new RuntimeException("Article publish failed: crawlerId=" + ca.getId(), e);
        }
    }

    /**
     * 发布单个产品（独立事务 + 乐观锁幂等 + 关联表全量写入）
     * 整个产品发布（主表+关联表）在同一事务中，任一失败全部回滚
     *
     * @return true=发布成功, false=CAS失败(已被其他线程发布)
     * @throws RuntimeException 数据库写入失败时抛出，触发事务回滚
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean publishSingleProduct(CrawlerProduct cp, Long defaultCategoryId) {
        // 乐观锁：CAS更新 synced 0→1
        int affected = crawlerProductMapper.update(null,
                new LambdaUpdateWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getId, cp.getId())
                        .eq(CrawlerProduct::getSynced, 0)
                        .set(CrawlerProduct::getSynced, 1)
                        .set(CrawlerProduct::getProductStatus, "PUBLISHING")
                        .set(CrawlerProduct::getUpdateTime, LocalDateTime.now())
        );
        if (affected == 0) {
            return false;
        }

        try {
            // ★ 先校验后发布：关键数据质量校验在写入正式表之前完成
            // 校验失败的产品标记为 PENDING_REVIEW，不创建 robot 记录
            PublishResult validation = preValidateProductData(cp);
            if (validation.needsReview) {
                markProductPendingReview(cp.getId(), String.join("; ", validation.warnings));
                log.warn("Product marked PENDING_REVIEW before publishing: crawlerId={}, reasons={}",
                        cp.getId(), validation.warnings);
                return true;
            }

            Long robotId;

            // 检查是否已有发布记录（重复发布场景）
            if (cp.getRobotIdSynced() != null && cp.getRobotIdSynced() > 0) {
                PublishRobot existing = publishRobotMapper.selectById(cp.getRobotIdSynced());
                if (existing != null) {
                    updateRobotFromCrawler(existing, cp);
                    publishRobotMapper.updateById(existing);
                    robotId = existing.getId();
                    // 删除旧关联数据后重新写入
                    deleteRelatedData(robotId);
                    saveRelatedData(robotId, cp);
                    markProductPublished(cp.getId(), robotId);
                    log.info("Updated existing robot: crawlerId={}, robotId={}", cp.getId(), robotId);
                    return true;
                }
            }

            // 新建机器人
            PublishRobot robot = convertToRobot(cp);
            publishRobotMapper.insert(robot);
            robotId = robot.getId();

            // 写入关联表（数据库写入异常必须抛出以回滚事务）
            saveRelatedData(robotId, cp);
            markProductPublished(cp.getId(), robotId);
            log.info("Published product: crawlerId={}, robotId={}, name={}", cp.getId(), robotId, cp.getProductName());
            return true;
        } catch (Exception e) {
            // 任何异常都回滚事务，由调用方标记 FAILED
            throw new RuntimeException("Product publish failed: crawlerId=" + cp.getId(), e);
        }
    }

    /**
     * 标记文章发布失败（独立事务，允许重试）
     * 重置 synced=0 以允许重新发布，记录失败原因和重试次数
     * 超过最大重试次数后标记为 MANUAL_REVIEW，需人工介入
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markArticleFailed(Long crawlerArticleId, String reason) {
        // 先查询当前重试次数
        CrawlerArticle current = crawlerArticleMapper.selectById(crawlerArticleId);
        int newRetryCount = (current != null && current.getRetryCount() != null)
                ? current.getRetryCount() + 1 : 1;

        if (newRetryCount > maxRetryCount) {
            // 超过最大重试次数，标记为需人工介入
            crawlerArticleMapper.update(null,
                    new LambdaUpdateWrapper<CrawlerArticle>()
                            .eq(CrawlerArticle::getId, crawlerArticleId)
                            .set(CrawlerArticle::getArticleStatus, "MANUAL_REVIEW")
                            .set(CrawlerArticle::getSynced, 0)
                            .set(CrawlerArticle::getFailReason, reason + " (已重试" + (newRetryCount - 1) + "次，需人工介入)")
                            .set(CrawlerArticle::getRetryCount, newRetryCount)
                            .set(CrawlerArticle::getUpdateTime, LocalDateTime.now())
            );
            log.warn("Article exceeded max retry count ({}), marked MANUAL_REVIEW: crawlerId={}, retryCount={}",
                    maxRetryCount, crawlerArticleId, newRetryCount);
        } else {
            // 计算指数退避：next_retry_time = NOW() + baseInterval * 2^(retryCount-1)
            LocalDateTime nextRetryTime = calculateNextRetryTime(newRetryCount);
            crawlerArticleMapper.update(null,
                    new LambdaUpdateWrapper<CrawlerArticle>()
                            .eq(CrawlerArticle::getId, crawlerArticleId)
                            .set(CrawlerArticle::getArticleStatus, "FAILED")
                            .set(CrawlerArticle::getSynced, 0)
                            .set(CrawlerArticle::getFailReason, reason)
                            .setSql("retry_count = retry_count + 1")
                            .set(CrawlerArticle::getNextRetryTime, nextRetryTime)
                            .set(CrawlerArticle::getUpdateTime, LocalDateTime.now())
            );
            log.warn("Article marked FAILED: crawlerId={}, reason={}, retryCount={}/{}, nextRetry={}",
                    crawlerArticleId, reason, newRetryCount, maxRetryCount, nextRetryTime);
        }
    }

    /**
     * 标记产品发布失败（独立事务，允许重试）
     * 重置 synced=0 以允许重新发布，记录失败原因和重试次数
     * 超过最大重试次数后标记为 MANUAL_REVIEW，需人工介入
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProductFailed(Long crawlerProductId, String reason) {
        // 先查询当前重试次数
        CrawlerProduct current = crawlerProductMapper.selectById(crawlerProductId);
        int newRetryCount = (current != null && current.getRetryCount() != null)
                ? current.getRetryCount() + 1 : 1;

        if (newRetryCount > maxRetryCount) {
            // 超过最大重试次数，标记为需人工介入
            crawlerProductMapper.update(null,
                    new LambdaUpdateWrapper<CrawlerProduct>()
                            .eq(CrawlerProduct::getId, crawlerProductId)
                            .set(CrawlerProduct::getProductStatus, "MANUAL_REVIEW")
                            .set(CrawlerProduct::getSynced, 0)
                            .set(CrawlerProduct::getFailReason, reason + " (已重试" + (newRetryCount - 1) + "次，需人工介入)")
                            .set(CrawlerProduct::getRetryCount, newRetryCount)
                            .set(CrawlerProduct::getUpdateTime, LocalDateTime.now())
            );
            log.warn("Product exceeded max retry count ({}), marked MANUAL_REVIEW: crawlerId={}, retryCount={}",
                    maxRetryCount, crawlerProductId, newRetryCount);
        } else {
            // 计算指数退避：next_retry_time = NOW() + baseInterval * 2^(retryCount-1)
            LocalDateTime nextRetryTime = calculateNextRetryTime(newRetryCount);
            crawlerProductMapper.update(null,
                    new LambdaUpdateWrapper<CrawlerProduct>()
                            .eq(CrawlerProduct::getId, crawlerProductId)
                            .set(CrawlerProduct::getProductStatus, "FAILED")
                            .set(CrawlerProduct::getSynced, 0)
                            .set(CrawlerProduct::getFailReason, reason)
                            .setSql("retry_count = retry_count + 1")
                            .set(CrawlerProduct::getNextRetryTime, nextRetryTime)
                            .set(CrawlerProduct::getUpdateTime, LocalDateTime.now())
            );
            log.warn("Product marked FAILED: crawlerId={}, reason={}, retryCount={}/{}, nextRetry={}",
                    crawlerProductId, reason, newRetryCount, maxRetryCount, nextRetryTime);
        }
    }

    /**
     * 标记文章为待审核状态（独立事务）
     * 用于数据质量问题（如JSON格式错误），需要人工审核而非直接发布
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markArticlePendingReview(Long crawlerArticleId, String reason) {
        crawlerArticleMapper.update(null,
                new LambdaUpdateWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getId, crawlerArticleId)
                        .set(CrawlerArticle::getArticleStatus, "PENDING_REVIEW")
                        .set(CrawlerArticle::getSynced, 0)
                        .set(CrawlerArticle::getFailReason, reason)
                        .set(CrawlerArticle::getUpdateTime, LocalDateTime.now())
        );
        log.info("Article marked PENDING_REVIEW: crawlerId={}, reason={}", crawlerArticleId, reason);
    }

    /**
     * 标记产品为待审核状态（独立事务）
     * 用于数据质量问题（如JSON格式错误），需要人工审核而非直接发布
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProductPendingReview(Long crawlerProductId, String reason) {
        crawlerProductMapper.update(null,
                new LambdaUpdateWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getId, crawlerProductId)
                        .set(CrawlerProduct::getProductStatus, "PENDING_REVIEW")
                        .set(CrawlerProduct::getSynced, 0)
                        .set(CrawlerProduct::getFailReason, reason)
                        .set(CrawlerProduct::getUpdateTime, LocalDateTime.now())
        );
        log.info("Product marked PENDING_REVIEW: crawlerId={}, reason={}", crawlerProductId, reason);
    }

    /**
     * 恢复超时的 PUBLISHING 状态（独立事务）
     * 防止任务永久停留在 PUBLISHING 状态
     * 使用 Java 计算指数退避时间，兼容 H2 和 MySQL
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recoverTimedOutPublishing(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        String failReason = "发布超时(>" + timeoutMinutes + "分钟未完成)";

        // 查询超时的文章，逐条更新（Java计算退避时间，兼容H2/MySQL）
        List<CrawlerArticle> timedOutArticles = crawlerArticleMapper.selectList(
                new LambdaQueryWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getArticleStatus, "PUBLISHING")
                        .lt(CrawlerArticle::getUpdateTime, threshold)
        );
        for (CrawlerArticle article : timedOutArticles) {
            int newRetryCount = (article.getRetryCount() != null ? article.getRetryCount() : 0) + 1;
            LocalDateTime nextRetryTime = calculateNextRetryTime(newRetryCount);
            crawlerArticleMapper.update(null,
                    new LambdaUpdateWrapper<CrawlerArticle>()
                            .eq(CrawlerArticle::getId, article.getId())
                            .set(CrawlerArticle::getArticleStatus, "FAILED")
                            .set(CrawlerArticle::getSynced, 0)
                            .set(CrawlerArticle::getRetryCount, newRetryCount)
                            .set(CrawlerArticle::getFailReason, failReason)
                            .set(CrawlerArticle::getNextRetryTime, nextRetryTime)
                            .set(CrawlerArticle::getUpdateTime, LocalDateTime.now())
            );
        }

        // 查询超时的产品，逐条更新
        List<CrawlerProduct> timedOutProducts = crawlerProductMapper.selectList(
                new LambdaQueryWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getProductStatus, "PUBLISHING")
                        .lt(CrawlerProduct::getUpdateTime, threshold)
        );
        for (CrawlerProduct product : timedOutProducts) {
            int newRetryCount = (product.getRetryCount() != null ? product.getRetryCount() : 0) + 1;
            LocalDateTime nextRetryTime = calculateNextRetryTime(newRetryCount);
            crawlerProductMapper.update(null,
                    new LambdaUpdateWrapper<CrawlerProduct>()
                            .eq(CrawlerProduct::getId, product.getId())
                            .set(CrawlerProduct::getProductStatus, "FAILED")
                            .set(CrawlerProduct::getSynced, 0)
                            .set(CrawlerProduct::getRetryCount, newRetryCount)
                            .set(CrawlerProduct::getFailReason, failReason)
                            .set(CrawlerProduct::getNextRetryTime, nextRetryTime)
                            .set(CrawlerProduct::getUpdateTime, LocalDateTime.now())
            );
        }

        int total = timedOutArticles.size() + timedOutProducts.size();
        if (total > 0) {
            log.warn("Recovered timed-out PUBLISHING records: articles={}, products={}",
                    timedOutArticles.size(), timedOutProducts.size());
        }
        return total;
    }

    // ==================== 私有方法 ====================

    /**
     * 计算指数退避的下次重试时间
     * 公式：next_retry_time = NOW() + baseInterval * 2^(retryCount-1)
     * retry 1: baseInterval * 1, retry 2: baseInterval * 2, retry 3: baseInterval * 4, ...
     */
    private LocalDateTime calculateNextRetryTime(int retryCount) {
        long delaySeconds = retryBaseIntervalSeconds * (1L << (retryCount - 1));
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    private void markArticlePublished(Long crawlerArticleId, Long articleId) {
        crawlerArticleMapper.update(null,
                new LambdaUpdateWrapper<CrawlerArticle>()
                        .eq(CrawlerArticle::getId, crawlerArticleId)
                        .set(CrawlerArticle::getArticleId, articleId)
                        .set(CrawlerArticle::getArticleStatus, "PUBLISHED")
                        .set(CrawlerArticle::getUpdateTime, LocalDateTime.now())
        );
    }

    private void markProductPublished(Long crawlerProductId, Long robotId) {
        crawlerProductMapper.update(null,
                new LambdaUpdateWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getId, crawlerProductId)
                        .set(CrawlerProduct::getRobotIdSynced, robotId)
                        .set(CrawlerProduct::getProductStatus, "PUBLISHED")
                        .set(CrawlerProduct::getUpdateTime, LocalDateTime.now())
        );
    }

    /**
     * 保存机器人关联数据（图片、价格、标签、视频、参数值）
     * ⚠️ 数据库写入异常直接抛出，触发事务回滚
     * ⚠️ JSON 格式错误由 preValidateProductData 预校验，此处不应再出现
     * ⚠️ 非关键警告（价格格式、参数定义缺失）仅记录日志，不影响发布状态
     *
     * @return 非关键警告列表
     */
    private PublishResult saveRelatedData(Long robotId, CrawlerProduct cp) {
        LocalDateTime now = LocalDateTime.now();
        PublishResult result = new PublishResult();

        // 1. 保存图片
        String gallery = cp.getGalleryLocal() != null ? cp.getGalleryLocal() : cp.getGallery();
        if (StringUtils.hasText(gallery)) {
            try {
                List<String> images = jsonMapper.readValue(gallery, new TypeReference<List<String>>() {});
                for (int i = 0; i < images.size(); i++) {
                    PublishRobotImage img = new PublishRobotImage();
                    img.setRobotId(robotId);
                    img.setUrl(images.get(i));
                    img.setType("gallery");
                    img.setSort(i + 1);
                    img.setCreateTime(now);
                    publishRobotImageMapper.insert(img);
                }
            } catch (Exception e) {
                // 先校验后发布：preValidateProductData 已保证 JSON 格式正确
                // 若此处仍解析失败，说明存在预校验未覆盖的边界情况，触发事务回滚
                throw new RuntimeException("Gallery JSON parse failed after pre-validation for robot " + robotId, e);
            }
        }
        String cover = cp.getCoverImageLocal() != null ? cp.getCoverImageLocal() : cp.getCoverImage();
        if (StringUtils.hasText(cover)) {
            PublishRobotImage coverImg = new PublishRobotImage();
            coverImg.setRobotId(robotId);
            coverImg.setUrl(cover);
            coverImg.setType("cover");
            coverImg.setSort(0);
            coverImg.setCreateTime(now);
            publishRobotImageMapper.insert(coverImg);
        }

        // 2. 保存价格
        if (StringUtils.hasText(cp.getPrice())) {
            try {
                BigDecimal priceVal = new BigDecimal(cp.getPrice().replaceAll("[^0-9.]", ""));
                PublishRobotPrice price = new PublishRobotPrice();
                price.setRobotId(robotId);
                price.setChannel("official");
                price.setRegion("CN");
                price.setPrice(priceVal);
                price.setUpdateTime(now);
                publishRobotPriceMapper.insert(price);
            } catch (NumberFormatException e) {
                // 价格格式错误属于非关键警告，不影响发布状态
                String warning = "Invalid price format: " + cp.getPrice();
                result.warnings.add(warning);
                log.warn("Invalid price format '{}' for robot {}, skipping: {}", cp.getPrice(), robotId, e.getMessage());
            }
        }

        // 3. 保存标签
        if (StringUtils.hasText(cp.getCategory())) {
            PublishRobotTag tag = new PublishRobotTag();
            tag.setRobotId(robotId);
            tag.setTagType("category");
            tag.setTagValue(cp.getCategory());
            tag.setCreateTime(now);
            publishRobotTagMapper.insert(tag);
        }

        // 4. 保存视频（数据库写入异常必须抛出）
        List<CrawlerMedia> videos = crawlerMediaMapper.selectList(
                new LambdaQueryWrapper<CrawlerMedia>()
                        .eq(CrawlerMedia::getRefProductId, cp.getId())
                        .eq(CrawlerMedia::getMediaType, "VIDEO")
                        .eq(CrawlerMedia::getDownloadStatus, "SUCCESS")
        );
        for (int i = 0; i < videos.size(); i++) {
            CrawlerMedia video = videos.get(i);
            PublishRobotVideo rv = new PublishRobotVideo();
            rv.setRobotId(robotId);
            rv.setTitle(cp.getProductName() + " 视频" + (i + 1));
            rv.setUrl(video.getStorageUrl() != null ? video.getStorageUrl() : video.getOriginalUrl());
            rv.setCover(null);
            rv.setDuration(0);
            rv.setSort(i + 1);
            rv.setCreateTime(now);
            publishRobotVideoMapper.insert(rv);
        }

        // 5. 保存参数值（数据库写入异常必须抛出）
        if (StringUtils.hasText(cp.getNormalizedParams())) {
            try {
                Map<String, String> params = jsonMapper.readValue(
                        cp.getNormalizedParams(), new TypeReference<Map<String, String>>() {});
                int paramSort = 0;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String paramName = entry.getKey();
                    String paramValue = entry.getValue();
                    if (StringUtils.hasText(paramValue)) {
                        PublishRobotParamDef paramDef = publishRobotParamDefMapper.selectOne(
                                new LambdaQueryWrapper<PublishRobotParamDef>()
                                        .eq(PublishRobotParamDef::getName, paramName)
                                        .last("LIMIT 1"));
                        if (paramDef != null) {
                            PublishRobotParamValue pv = new PublishRobotParamValue();
                            pv.setRobotId(robotId);
                            pv.setDefId(paramDef.getId());
                            pv.setValue(paramValue);
                            pv.setCreateTime(now);
                            pv.setUpdateTime(now);
                            publishRobotParamValueMapper.insert(pv);
                            paramSort++;
                        } else {
                            // 参数定义不存在属于配置问题，记录为非关键警告
                            String warning = "Parameter definition not found: " + paramName;
                            result.warnings.add(warning);
                            log.debug("Parameter definition not found for '{}', skipping for robot {}", paramName, robotId);
                        }
                    }
                }
                if (paramSort > 0) {
                    log.info("Saved {} parameter values for robot {}", paramSort, robotId);
                }
            } catch (Exception e) {
                // 先校验后发布：preValidateProductData 已保证 JSON 格式正确
                // 若此处仍解析失败，说明存在预校验未覆盖的边界情况，触发事务回滚
                throw new RuntimeException("NormalizedParams JSON parse failed after pre-validation for robot " + robotId, e);
            }
        }

        return result;
    }

    private void deleteRelatedData(Long robotId) {
        publishRobotImageMapper.delete(
                new LambdaQueryWrapper<PublishRobotImage>().eq(PublishRobotImage::getRobotId, robotId));
        publishRobotPriceMapper.delete(
                new LambdaQueryWrapper<PublishRobotPrice>().eq(PublishRobotPrice::getRobotId, robotId));
        publishRobotTagMapper.delete(
                new LambdaQueryWrapper<PublishRobotTag>().eq(PublishRobotTag::getRobotId, robotId));
        publishRobotVideoMapper.delete(
                new LambdaQueryWrapper<PublishRobotVideo>().eq(PublishRobotVideo::getRobotId, robotId));
        publishRobotParamValueMapper.delete(
                new LambdaQueryWrapper<PublishRobotParamValue>().eq(PublishRobotParamValue::getRobotId, robotId));
    }

    /**
     * 预校验产品数据质量：在写入正式表之前检查关键数据格式
     * 校验失败的产品将标记为 PENDING_REVIEW，不创建 robot 记录
     *
     * 校验项：
     * - gallery JSON 格式（图集数据）
     * - normalizedParams JSON 格式（参数数据）
     *
     * 非关键校验（价格格式、参数定义缺失）在 saveRelatedData 中处理，不影响发布状态
     */
    private PublishResult preValidateProductData(CrawlerProduct cp) {
        PublishResult result = new PublishResult();

        // 1. 校验 gallery JSON 格式
        String gallery = cp.getGalleryLocal() != null ? cp.getGalleryLocal() : cp.getGallery();
        if (StringUtils.hasText(gallery)) {
            try {
                jsonMapper.readValue(gallery, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                String warning = "Invalid gallery JSON: " + e.getMessage();
                result.warnings.add(warning);
                result.needsReview = true;
                log.warn("Pre-validation: invalid gallery JSON for product {}: {}", cp.getProductName(), e.getMessage());
            }
        }

        // 2. 校验 normalizedParams JSON 格式
        if (StringUtils.hasText(cp.getNormalizedParams())) {
            try {
                jsonMapper.readValue(cp.getNormalizedParams(), new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                String warning = "Invalid normalizedParams JSON: " + e.getMessage();
                result.warnings.add(warning);
                result.needsReview = true;
                log.warn("Pre-validation: invalid normalizedParams JSON for product {}: {}", cp.getProductName(), e.getMessage());
            }
        }

        return result;
    }

    private PublishArticle convertToArticle(CrawlerArticle ca, Long defaultCategoryId) {
        PublishArticle article = new PublishArticle();
        article.setCategoryId(resolveArticleCategory(ca, defaultCategoryId));
        article.setBrandId(ca.getBrandId());
        article.setRobotId(ca.getRobotId());
        article.setTitle(ca.getTitle());
        article.setCover(ca.getCoverImageLocal() != null ? ca.getCoverImageLocal() : ca.getCoverImage());
        article.setSummary(ca.getSummary());
        article.setContent(ca.getContentHtml());
        article.setAuthor(ca.getAuthor());
        article.setSource(ca.getSourceName() != null ? ca.getSourceName() : ca.getSourceSite());
        article.setSourceUrl(ca.getSourceUrl());
        article.setTags(ca.getTags());
        article.setIsTop(0);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setFavoriteCount(0);
        article.setCommentCount(0);
        article.setStatus(1);
        article.setPublishTime(ca.getPublishTime() != null ? ca.getPublishTime() : LocalDateTime.now());
        article.setIsExample(0);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        return article;
    }

    private void updateArticleFromCrawler(PublishArticle article, CrawlerArticle ca, Long defaultCategoryId) {
        article.setCategoryId(resolveArticleCategory(ca, defaultCategoryId));
        article.setBrandId(ca.getBrandId());
        article.setRobotId(ca.getRobotId());
        article.setTitle(ca.getTitle());
        article.setCover(ca.getCoverImageLocal() != null ? ca.getCoverImageLocal() : ca.getCoverImage());
        article.setSummary(ca.getSummary());
        article.setContent(ca.getContentHtml());
        article.setAuthor(ca.getAuthor());
        article.setSource(ca.getSourceName() != null ? ca.getSourceName() : ca.getSourceSite());
        article.setSourceUrl(ca.getSourceUrl());
        article.setTags(ca.getTags());
        article.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 解析文章分类：优先匹配采集文章分类，再按名称匹配栏目表，最后使用默认分类
     * 默认分类不存在时返回 null（调用方应转入待审核）
     */
    private Long resolveArticleCategory(CrawlerArticle ca, Long defaultCategoryId) {
        // 1. 优先使用采集文章已匹配的分类ID
        if (ca.getCategoryId() != null && ca.getCategoryId() > 0) {
            return ca.getCategoryId();
        }

        // 2. 按分类名称匹配 article_category 表
        if (StringUtils.hasText(ca.getCategory())) {
            try {
                ArticleCategory matched = articleCategoryMapper.selectOne(
                        new LambdaQueryWrapper<ArticleCategory>()
                                .eq(ArticleCategory::getName, ca.getCategory())
                                .eq(ArticleCategory::getStatus, 1)
                                .last("LIMIT 1")
                );
                if (matched != null) {
                    return matched.getId();
                }
                // 模糊匹配：分类名称包含文章分类关键词
                matched = articleCategoryMapper.selectOne(
                        new LambdaQueryWrapper<ArticleCategory>()
                                .like(ArticleCategory::getName, ca.getCategory())
                                .eq(ArticleCategory::getStatus, 1)
                                .last("LIMIT 1")
                );
                if (matched != null) {
                    return matched.getId();
                }
                // 反向模糊匹配：文章分类包含栏目名称
                matched = articleCategoryMapper.selectOne(
                        new LambdaQueryWrapper<ArticleCategory>()
                                .eq(ArticleCategory::getStatus, 1)
                                .last("LIMIT 1")
                );
                // 遍历所有启用的栏目，检查文章分类是否包含栏目名称
                List<ArticleCategory> allCategories = articleCategoryMapper.selectList(
                        new LambdaQueryWrapper<ArticleCategory>()
                                .eq(ArticleCategory::getStatus, 1)
                );
                for (ArticleCategory cat : allCategories) {
                    if (ca.getCategory().contains(cat.getName())) {
                        return cat.getId();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to match article category by name: {}", e.getMessage());
            }
        }

        // 3. 使用配置的默认分类
        return defaultCategoryId;
    }

    private PublishRobot convertToRobot(CrawlerProduct cp) {
        PublishRobot robot = new PublishRobot();
        robot.setCategoryId(cp.getCategoryId());
        robot.setSeriesId(cp.getSeriesId());
        robot.setBrandId(cp.getBrandId());
        robot.setName(cp.getProductName());
        robot.setModel(cp.getModel());
        robot.setSubtitle(cp.getSummary());
        robot.setCoverImage(cp.getCoverImageLocal() != null ? cp.getCoverImageLocal() : cp.getCoverImage());
        robot.setImages(cp.getGalleryLocal() != null ? cp.getGalleryLocal() : cp.getGallery());
        robot.setMainParams(cp.getNormalizedParams());
        robot.setDataSource("CRAWLER");
        robot.setSourceUrl(cp.getSourceUrl());
        robot.setSourceName(cp.getSourceName());
        robot.setReleaseDate(cp.getReleaseDate());
        robot.setStatus(1);
        robot.setIsExample(0);
        robot.setCreateTime(LocalDateTime.now());
        robot.setUpdateTime(LocalDateTime.now());
        return robot;
    }

    private void updateRobotFromCrawler(PublishRobot robot, CrawlerProduct cp) {
        robot.setCategoryId(cp.getCategoryId());
        robot.setSeriesId(cp.getSeriesId());
        robot.setBrandId(cp.getBrandId());
        robot.setName(cp.getProductName());
        robot.setModel(cp.getModel());
        robot.setSubtitle(cp.getSummary());
        robot.setCoverImage(cp.getCoverImageLocal() != null ? cp.getCoverImageLocal() : cp.getCoverImage());
        robot.setImages(cp.getGalleryLocal() != null ? cp.getGalleryLocal() : cp.getGallery());
        robot.setMainParams(cp.getNormalizedParams());
        robot.setDataSource("CRAWLER");
        robot.setSourceUrl(cp.getSourceUrl());
        robot.setSourceName(cp.getSourceName());
        robot.setReleaseDate(cp.getReleaseDate());
        robot.setUpdateTime(LocalDateTime.now());
    }
}