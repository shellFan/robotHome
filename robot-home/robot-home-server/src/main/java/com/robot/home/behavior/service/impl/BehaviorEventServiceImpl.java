package com.robot.home.behavior.service.impl;

import cn.hutool.core.util.StrUtil;
import com.robot.home.behavior.dto.BehaviorEventDTO;
import com.robot.home.behavior.entity.BehaviorEvent;
import com.robot.home.behavior.mapper.BehaviorEventMapper;
import com.robot.home.behavior.service.BehaviorEventService;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.ranking.entity.RankingWeight;
import com.robot.home.ranking.mapper.RankingWeightMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 行为事件服务实现
 * <p>
 * 热度计算：
 * 1. 每次行为事件触发 Redis ZINCRBY，权重从 ranking_weight 表读取
 * 2. Redis Key: robot:hot:{bizType}，Value: ZSET(member=bizId, score=热度分)
 * 3. 权重配置化，不硬编码
 * <p>
 * 防刷策略：
 * 1. 同一用户+同一对象+同一事件类型，60秒内不重复计数
 * 2. Redis Key: robot:hot:dedup:{eventType}:{bizType}:{bizId}:{userId}，TTL=60s
 */
@Service
public class BehaviorEventServiceImpl implements BehaviorEventService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorEventServiceImpl.class);

    /** Redis ZSET key 前缀: robot:hot:{bizType} */
    private static final String HOT_ZSET_PREFIX = "robot:hot:";
    /** 去重 key 前缀: robot:hot:dedup:{eventType}:{bizType}:{bizId}:{userId} */
    private static final String DEDUP_PREFIX = "robot:hot:dedup:";
    /** 去重窗口（秒） */
    private static final long DEDUP_WINDOW_SECONDS = 60L;
    /** ZSET 默认过期时间（7天，排行榜快照任务会刷新） */
    private static final long ZSET_EXPIRE_DAYS = 7L;

    /** 事件类型 -> 权重（启动时从DB加载，运行时可通过管理接口刷新） */
    private volatile Map<String, Integer> weightMap = new HashMap<>();

    @Resource
    private BehaviorEventMapper behaviorEventMapper;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private RankingWeightMapper rankingWeightMapper;

    /** 权重缓存时间戳 */
    private volatile long weightCacheTime = 0;
    /** 权重缓存有效期（5分钟） */
    private static final long WEIGHT_CACHE_TTL_MS = 5 * 60 * 1000L;

    /**
     * 启动时自动从DB加载权重配置
     */
    @PostConstruct
    public void init() {
        loadWeights();
    }

    /**
     * 从 ranking_weight 表加载权重到内存
     */
    private void loadWeights() {
        try {
            List<RankingWeight> weights = rankingWeightMapper.selectList(null);
            if (!weights.isEmpty()) {
                Map<String, Integer> newMap = new HashMap<>();
                for (RankingWeight w : weights) {
                    newMap.put(w.getEventType(), w.getWeight());
                }
                this.weightMap = newMap;
                log.info("行为事件权重配置已加载: {}", newMap);
            }
            weightCacheTime = System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("加载行为事件权重失败，使用默认值: {}", e.getMessage());
            weightCacheTime = System.currentTimeMillis();
        }
    }

    /**
     * 确保权重已加载（带5分钟缓存刷新）
     */
    private void ensureWeightsLoaded() {
        long now = System.currentTimeMillis();
        if (now - weightCacheTime < WEIGHT_CACHE_TTL_MS) {
            return;
        }
        loadWeights();
    }

    /**
     * 初始化权重配置（管理接口可调用刷新）
     */
    public void initWeights(Map<String, Integer> weights) {
        Map<String, Integer> newMap = new HashMap<>(weights);
        this.weightMap = newMap;
        weightCacheTime = System.currentTimeMillis();
        log.info("行为事件权重配置已刷新: {}", newMap);
    }

    @Override
    public void record(Long userId, BehaviorEventDTO dto, String ip, String ua, String sessionId) {
        // 1. 参数校验
        validate(dto);

        // 2. 异步写入MySQL
        saveEvent(userId, dto, ip, ua);

        // 3. Redis热度更新
        updateHotScore(userId, dto, sessionId);
    }

    @Override
    @Async("asyncExecutor")
    public void batchRecord(Long userId, List<BehaviorEventDTO> events, String ip, String ua, String sessionId) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (BehaviorEventDTO dto : events) {
            try {
                record(userId, dto, ip, ua, sessionId);
            } catch (Exception e) {
                log.warn("批量记录行为事件失败: eventType={}, bizType={}, bizId={}, error={}",
                        dto.getEventType(), dto.getBizType(), dto.getBizId(), e.getMessage());
            }
        }
    }

    @Override
    public double getHotScore(String bizType, Long bizId) {
        String key = HOT_ZSET_PREFIX + bizType;
        Double score = redisUtils.zScore(key, String.valueOf(bizId));
        return score == null ? 0.0 : score;
    }

    @Override
    public List<Map<String, Object>> getTopHot(String bizType, int limit) {
        String key = HOT_ZSET_PREFIX + bizType;
        int size = Math.max(1, Math.min(limit, 100));
        // ZREVRANGE 按score降序
        Set<String> members = redisUtils.zReverseRange(key, 0, size - 1);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (String member : members) {
            Double score = redisUtils.zScore(key, member);
            Map<String, Object> item = new HashMap<>();
            item.put("id", Long.valueOf(member));
            item.put("score", score == null ? 0 : score);
            item.put("rank", rank++);
            result.add(item);
        }
        return result;
    }

    /**
     * 参数校验：事件类型和业务类型白名单
     */
    private void validate(BehaviorEventDTO dto) {
        if (!VALID_EVENT_TYPES.contains(dto.getEventType())) {
            throw new BusinessException("不支持的事件类型: " + dto.getEventType());
        }
        if (dto.getBizType() != null && !VALID_BIZ_TYPES.contains(dto.getBizType())) {
            throw new BusinessException("不支持的业务类型: " + dto.getBizType());
        }
        if (StrUtil.isNotBlank(dto.getExtra()) && dto.getExtra().length() > 512) {
            throw new BusinessException("扩展信息过长");
        }
    }

    /**
     * 异步写入MySQL
     */
    @Async("asyncExecutor")
    public void saveEvent(Long userId, BehaviorEventDTO dto, String ip, String ua) {
        try {
            BehaviorEvent event = new BehaviorEvent();
            event.setUserId(userId);
            event.setEventType(dto.getEventType());
            event.setBizType(dto.getBizType());
            event.setBizId(dto.getBizId());
            event.setExtra(StrUtil.isBlank(dto.getExtra()) ? null : dto.getExtra());
            event.setIp(ip != null && ip.length() > 64 ? ip.substring(0, 64) : ip);
            event.setUserAgent(ua != null && ua.length() > 512 ? ua.substring(0, 512) : ua);
            event.setCreateTime(new Date());
            behaviorEventMapper.insert(event);
        } catch (Exception e) {
            log.error("保存行为事件失败: eventType={}, bizType={}, bizId={}",
                    dto.getEventType(), dto.getBizType(), dto.getBizId(), e);
        }
    }

    /**
     * Redis热度更新
     * <p>
     * 1. 去重检查：同一用户+同一对象+同一事件，60秒内不重复计数
     * 2. 权重查找：从内存 weightMap 获取事件权重
     * 3. ZINCRBY：增加热度分
     */
    private void updateHotScore(Long userId, BehaviorEventDTO dto, String sessionId) {
        if (dto.getBizType() == null || dto.getBizId() == null) {
            return; // 搜索等事件无业务对象，不更新热度
        }

        // 去重检查：同一用户/会话+同一对象+同一事件，60秒内不重复计数
        String dedupIdentity = userId != null ? String.valueOf(userId) : "session:" + sessionId;
        String dedupKey = DEDUP_PREFIX + dto.getEventType() + ":" + dto.getBizType()
                + ":" + dto.getBizId() + ":" + dedupIdentity;
        if (redisUtils.hasKey(dedupKey)) {
            return; // 60秒内已记录过，跳过
        }
        redisUtils.set(dedupKey, "1", DEDUP_WINDOW_SECONDS, TimeUnit.SECONDS);

        // 确保权重已加载
        ensureWeightsLoaded();

        // 获取权重
        Integer weight = weightMap.get(dto.getEventType());
        if (weight == null) {
            weight = 1; // 默认权重1
        }

        // UNFAVORITE 是负向事件
        if ("UNFAVORITE".equals(dto.getEventType())) {
            weight = -Math.abs(weight);
        }

        // ZINCRBY 更新热度
        String zsetKey = HOT_ZSET_PREFIX + dto.getBizType();
        redisUtils.zIncrBy(zsetKey, String.valueOf(dto.getBizId()), weight.doubleValue());

        // 设置ZSET过期时间（避免永久占用内存）
        Long ttl = redisUtils.getExpire(zsetKey);
        if (ttl == null || ttl < 0) {
            redisUtils.expire(zsetKey, ZSET_EXPIRE_DAYS, TimeUnit.DAYS);
        }
    }
}