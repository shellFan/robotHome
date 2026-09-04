package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robot.home.collector.entity.CrawlerUrl;
import com.robot.home.collector.mapper.CrawlerUrlMapper;
import com.robot.home.collector.util.HashUtils;
import com.robot.home.collector.util.UrlNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 去重服务
 * URL去重（规范化+SHA-256） + 内容去重（SimHash）
 */
@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);

    private static final String URL_SET_PREFIX = "crawler:url:";
    private static final String SIMHASH_PREFIX = "crawler:simhash:";
    private static final String CONTENT_HASH_PREFIX = "crawler:content_hash:";

    private static final int SIMHASH_THRESHOLD = 3;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CrawlerUrlMapper urlMapper;

    /**
     * 检查URL是否已存在
     * 先查Redis缓存，再查数据库
     * 只对成功抓取的URL返回true，允许重试失败的URL
     */
    public boolean isUrlDuplicate(String rawUrl) {
        String normalizedUrl = UrlNormalizer.normalize(rawUrl);
        String urlHash = UrlNormalizer.hash(normalizedUrl);

        // 1. 查Redis
        if (redisTemplate != null) {
            try {
                Boolean exists = redisTemplate.hasKey(URL_SET_PREFIX + urlHash);
                if (exists != null && exists) {
                    // Redis中存在，进一步检查状态值
                    String status = redisTemplate.opsForValue().get(URL_SET_PREFIX + urlHash);
                    if ("SUCCESS".equals(status)) {
                        return true;
                    }
                    // 非SUCCESS状态允许重试
                    return false;
                }
            } catch (Exception e) {
                log.debug("Redis查询失败，降级到数据库查询: {}", e.getMessage());
            }
        }

        // 2. 查数据库 - 只检查成功状态的URL
        Long count = urlMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrawlerUrl>()
                        .eq(CrawlerUrl::getUrlHash, urlHash)
                        .eq(CrawlerUrl::getUrlStatus, "SUCCESS"));
        return count != null && count > 0;
    }

    /**
     * 记录URL已抓取（默认标记为SUCCESS）
     * 同时写入Redis缓存和数据库，确保跨任务去重一致性
     */
    public void markUrlFetched(String rawUrl) {
        markUrlFetched(rawUrl, "SUCCESS");
    }

    /**
     * 记录URL已抓取，指定状态
     * 同时写入Redis缓存和数据库，确保跨任务去重一致性
     */
    public void markUrlFetched(String rawUrl, String status) {
        String normalizedUrl = UrlNormalizer.normalize(rawUrl);
        String urlHash = UrlNormalizer.hash(normalizedUrl);

        // 1. 写入Redis缓存（7天过期）- 存储状态值
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(URL_SET_PREFIX + urlHash, status, 7, TimeUnit.DAYS);
            } catch (Exception e) {
                log.debug("Redis写入失败，跳过缓存: {}", e.getMessage());
            }
        }

        // 2. 写入或更新数据库（持久化，确保跨任务去重）
        try {
            CrawlerUrl existing = urlMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrawlerUrl>()
                            .eq(CrawlerUrl::getUrlHash, urlHash)
                            .last("LIMIT 1"));
            if (existing == null) {
                CrawlerUrl crawlerUrl = new CrawlerUrl();
                crawlerUrl.setUrl(normalizedUrl);
                crawlerUrl.setNormalizedUrl(normalizedUrl);
                crawlerUrl.setUrlHash(urlHash);
                crawlerUrl.setUrlStatus(status);
                crawlerUrl.setCreateTime(java.time.LocalDateTime.now());
                crawlerUrl.setUpdateTime(java.time.LocalDateTime.now());
                urlMapper.insert(crawlerUrl);
            } else {
                // 更新状态（允许从FAILED更新为SUCCESS）
                existing.setUrlStatus(status);
                existing.setUpdateTime(java.time.LocalDateTime.now());
                urlMapper.updateById(existing);
            }
        } catch (Exception e) {
            log.debug("数据库写入URL记录失败: {}", e.getMessage());
        }
    }

    /**
     * 检查内容是否重复（SHA-256精确匹配）
     */
    public boolean isContentDuplicate(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }

        String contentHash = HashUtils.sha256(content);

        if (redisTemplate != null) {
            try {
                Boolean exists = redisTemplate.hasKey(CONTENT_HASH_PREFIX + contentHash);
                if (exists != null && exists) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("Redis查询失败，跳过内容去重缓存: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     * 记录内容哈希
     */
    public void markContentFetched(String content) {
        if (StringUtils.isBlank(content)) {
            return;
        }

        String contentHash = HashUtils.sha256(content);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(CONTENT_HASH_PREFIX + contentHash, "1", 30, TimeUnit.DAYS);
            } catch (Exception e) {
                log.debug("Redis写入失败，跳过内容哈希缓存: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查内容是否相似（SimHash近似匹配）
     * 将64位SimHash分为4段存储，至少1段匹配才可能相似
     */
    public boolean isContentSimilar(String content) {
        if (StringUtils.isBlank(content) || redisTemplate == null) {
            return false;
        }

        long simHash = HashUtils.simHash(content);
        int[] segments = HashUtils.simHashSegments(simHash);

        for (int i = 0; i < 4; i++) {
            String segKey = HashUtils.simHashSegmentKey(i, segments[i]);
            try {
                String existingHash = redisTemplate.opsForValue().get(segKey);
                if (existingHash != null) {
                    try {
                        long existingSimHash = Long.parseLong(existingHash);
                        if (HashUtils.isSimilar(simHash, existingSimHash, SIMHASH_THRESHOLD)) {
                            log.debug("Similar content found via SimHash segment {} (distance={})",
                                    i, HashUtils.hammingDistance(simHash, existingSimHash));
                            return true;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } catch (Exception e) {
                log.debug("Redis查询SimHash失败，跳过: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     * 记录SimHash
     */
    public void markSimHash(String content) {
        if (StringUtils.isBlank(content) || redisTemplate == null) {
            return;
        }

        long simHash = HashUtils.simHash(content);
        int[] segments = HashUtils.simHashSegments(simHash);

        for (int i = 0; i < 4; i++) {
            String segKey = HashUtils.simHashSegmentKey(i, segments[i]);
            try {
                redisTemplate.opsForValue().set(segKey, String.valueOf(simHash), 30, TimeUnit.DAYS);
            } catch (Exception e) {
                log.debug("Redis写入SimHash失败，跳过: {}", e.getMessage());
            }
        }
    }

    /**
     * 综合去重检查
     * @return null表示不重复，否则返回重复原因
     */
    public String checkDuplicate(String url, String content) {
        // URL去重
        if (isUrlDuplicate(url)) {
            return "URL_DUPLICATE";
        }

        // 内容精确去重
        if (StringUtils.isNotBlank(content) && isContentDuplicate(content)) {
            return "CONTENT_DUPLICATE";
        }

        // SimHash相似度去重
        if (StringUtils.isNotBlank(content) && isContentSimilar(content)) {
            return "CONTENT_SIMILAR";
        }

        return null;
    }

    /**
     * 标记URL和内容已抓取
     */
    public void markFetched(String url, String content) {
        markUrlFetched(url, "SUCCESS");  // 显式标记SUCCESS
        if (StringUtils.isNotBlank(content)) {
            markContentFetched(content);
            markSimHash(content);
        }
    }

    /**
     * 清除所有去重数据（URL记录、内容哈希、SimHash的Redis缓存和数据库记录）
     * 用于重新抓取已处理过的URL
     */
    public int clearAllDedupData() {
        int deleted = 0;
        // 1. 清除数据库中的URL去重记录
        try {
            deleted = urlMapper.delete(new LambdaQueryWrapper<>());
            log.info("Cleared {} URL dedup records from database", deleted);
        } catch (Exception e) {
            log.warn("Failed to clear URL dedup records from database: {}", e.getMessage());
        }
        // 2. 清除Redis中的所有去重缓存（URL + 内容哈希 + SimHash）
        if (redisTemplate != null) {
            try {
                int totalCleared = 0;
                // 清除URL去重键
                java.util.Set<String> urlKeys = redisTemplate.keys(URL_SET_PREFIX + "*");
                if (urlKeys != null && !urlKeys.isEmpty()) {
                    redisTemplate.delete(urlKeys);
                    totalCleared += urlKeys.size();
                    log.info("Cleared {} URL dedup keys from Redis", urlKeys.size());
                }
                // 清除内容哈希键
                java.util.Set<String> contentHashKeys = redisTemplate.keys(CONTENT_HASH_PREFIX + "*");
                if (contentHashKeys != null && !contentHashKeys.isEmpty()) {
                    redisTemplate.delete(contentHashKeys);
                    totalCleared += contentHashKeys.size();
                    log.info("Cleared {} content hash keys from Redis", contentHashKeys.size());
                }
                // 清除SimHash段键
                java.util.Set<String> simHashKeys = redisTemplate.keys(SIMHASH_PREFIX + "*");
                if (simHashKeys != null && !simHashKeys.isEmpty()) {
                    redisTemplate.delete(simHashKeys);
                    totalCleared += simHashKeys.size();
                    log.info("Cleared {} simhash keys from Redis", simHashKeys.size());
                }
                log.info("Total cleared {} dedup keys from Redis", totalCleared);
            } catch (Exception e) {
                log.warn("Failed to clear Redis dedup cache: {}", e.getMessage());
            }
        }
        return deleted;
    }
}