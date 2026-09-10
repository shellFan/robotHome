package com.robot.home.ranking.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.ranking.entity.RankingDecayConfig;
import com.robot.home.ranking.entity.RankingSnapshot;
import com.robot.home.ranking.entity.RankingWeight;
import com.robot.home.ranking.mapper.RankingDecayConfigMapper;
import com.robot.home.ranking.mapper.RankingSnapshotMapper;
import com.robot.home.ranking.mapper.RankingWeightMapper;
import com.robot.home.ranking.service.RankingService;
import com.robot.home.ranking.vo.RankingTypeVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotCategory;
import com.robot.home.robot.mapper.RobotCategoryMapper;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.service.RobotService;
import com.robot.home.robot.vo.RobotListVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 排行榜实现
 * <p>
 * Phase6 增强特性：
 * 1. 权重配置化：从 ranking_weight 表读取，不硬编码
 * 2. 时间衰减：从 ranking_decay_config 表读取半衰期，公式 score × 2^(-elapsed/halfLife)
 * 3. Redis ZSET 实时热度：行为事件系统实时更新
 * 4. MySQL 快照：定时持久化到 ranking_snapshot 表
 */
@Service
public class RankingServiceImpl implements RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingServiceImpl.class);

    /** 榜单类型 → 一级分类 id */
    private static final Map<String, Long> TYPE_CATEGORY = new HashMap<>();

    static {
        TYPE_CATEGORY.put(Constants.RANK_HUMANOID, 1L);
        TYPE_CATEGORY.put(Constants.RANK_QUADRUPED, 2L);
        TYPE_CATEGORY.put(Constants.RANK_SERVICE, 3L);
        TYPE_CATEGORY.put(Constants.RANK_FAMILY, 4L);
        TYPE_CATEGORY.put(Constants.RANK_INDUSTRIAL, 5L);
        TYPE_CATEGORY.put(Constants.RANK_DEV, 9L);
    }

    /** 默认权重（DB加载失败时降级使用） */
    private static final Map<String, Integer> DEFAULT_WEIGHTS = new HashMap<>();

    static {
        DEFAULT_WEIGHTS.put("VIEW", 1);
        DEFAULT_WEIGHTS.put("FAVORITE", 8);
        DEFAULT_WEIGHTS.put("COMPARE", 12);
        DEFAULT_WEIGHTS.put("INQUIRY", 30);
        DEFAULT_WEIGHTS.put("COMMENT", 5);
        DEFAULT_WEIGHTS.put("SCORE", 10);
        DEFAULT_WEIGHTS.put("NEW_PRODUCT", 2);
    }

    /** 默认衰减配置（DB加载失败时降级使用） */
    private static final int DEFAULT_HALF_LIFE_DAYS = 90;
    private static final double DEFAULT_MIN_DECAY = 0.1;

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotCategoryMapper categoryMapper;
    @Resource
    private RobotService robotService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private RankingWeightMapper rankingWeightMapper;
    @Resource
    private RankingDecayConfigMapper rankingDecayConfigMapper;
    @Resource
    private RankingSnapshotMapper rankingSnapshotMapper;

    @Value("${robot.ranking-cache:600}")
    private long rankingCacheSeconds;

    /** 内存缓存：事件类型 → 权重 */
    private volatile Map<String, Integer> weightCache = new HashMap<>(DEFAULT_WEIGHTS);
    /** 内存缓存：榜单类型 → 衰减配置 */
    private volatile Map<String, RankingDecayConfig> decayCache = new HashMap<>();
    /** 权重缓存时间戳 */
    private volatile long weightCacheTime = 0;
    /** 权重缓存有效期（5分钟） */
    private static final long WEIGHT_CACHE_TTL_MS = 5 * 60 * 1000L;

    @Override
    public List<RobotListVO> rank(String type, int limit, Long currentUserId) {
        String rankType = StrUtil.isBlank(type) ? Constants.RANK_HOT : type;
        int size = Math.max(1, Math.min(limit, 100));

        String cacheKey = Constants.CACHE_RANKING_PREFIX + rankType + ":" + size;
        List<Long> ids = parseIds(redisUtils.get(cacheKey));
        if (ids == null || ids.isEmpty()) {
            Long categoryId = TYPE_CATEGORY.get(rankType);
            List<Long> categoryIds = null;
            if (categoryId != null) {
                categoryIds = new ArrayList<>();
                categoryIds.add(categoryId);
                List<RobotCategory> children = categoryMapper.selectList(Wrappers.<RobotCategory>lambdaQuery()
                        .eq(RobotCategory::getParentId, categoryId)
                        .eq(RobotCategory::getStatus, 1));
                for (RobotCategory c : children) {
                    categoryIds.add(c.getId());
                }
            }
            List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                    .in(categoryIds != null && !categoryIds.isEmpty(), Robot::getCategoryId, categoryIds));
            // 使用配置化权重 + 时间衰减计算热度
            robots.sort((a, b) -> Long.compare(hotScore(b, rankType), hotScore(a, rankType)));
            ids = new ArrayList<>();
            for (int i = 0; i < robots.size() && ids.size() < size; i++) {
                ids.add(robots.get(i).getId());
            }
            if (!ids.isEmpty()) {
                redisUtils.set(cacheKey, joinIds(ids), rankingCacheSeconds, TimeUnit.SECONDS);
            }
        }
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<RobotListVO> list = robotMapper.selectListByIds(ids);
        Map<Long, RobotListVO> index = new HashMap<>();
        for (RobotListVO vo : list) {
            index.put(vo.getId(), vo);
        }
        List<RobotListVO> ordered = new ArrayList<>();
        for (Long id : ids) {
            RobotListVO vo = index.get(id);
            if (vo != null) {
                ordered.add(vo);
            }
        }
        return ordered;
    }

    @Override
    public List<RankingTypeVO> types() {
        return Arrays.asList(
                new RankingTypeVO(Constants.RANK_HOT, "热门机器人榜"),
                new RankingTypeVO(Constants.RANK_HUMANOID, "人形机器人榜"),
                new RankingTypeVO(Constants.RANK_QUADRUPED, "机器狗榜"),
                new RankingTypeVO(Constants.RANK_SERVICE, "服务机器人榜"),
                new RankingTypeVO(Constants.RANK_INDUSTRIAL, "工业机器人榜"),
                new RankingTypeVO(Constants.RANK_FAMILY, "家庭机器人榜"),
                new RankingTypeVO(Constants.RANK_DEV, "开发机器人榜"));
    }

    @Override
    public int refreshHotScores() {
        ensureWeightsLoaded();
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery().orderByAsc(Robot::getId));
        int updated = 0;
        for (Robot r : robots) {
            long score = hotScore(r, Constants.RANK_HOT);
            Robot update = new Robot();
            update.setId(r.getId());
            update.setHotScore(score);
            robotMapper.updateById(update);
            updated++;
        }
        // 刷新后清空榜单缓存
        for (String key : new ArrayList<>(redisUtils.keys(Constants.CACHE_RANKING_PREFIX + "*"))) {
            redisUtils.delete(key);
        }
        return updated;
    }

    /**
     * 将 Redis ZSET 排行榜持久化到 ranking_snapshot 表
     * 由定时任务调用
     */
    public int snapshotRankings() {
        String[] rankTypes = {Constants.RANK_HOT, Constants.RANK_HUMANOID, Constants.RANK_QUADRUPED,
                Constants.RANK_SERVICE, Constants.RANK_INDUSTRIAL, Constants.RANK_FAMILY, Constants.RANK_DEV};
        Date now = new Date();
        int count = 0;
        for (String rankType : rankTypes) {
            String zsetKey = Constants.CACHE_HOT_PREFIX + rankType;
            Set<String> members = redisUtils.zReverseRange(zsetKey, 0, 99);
            if (members == null || members.isEmpty()) {
                continue;
            }
            int rank = 1;
            for (String member : members) {
                Double score = redisUtils.zScore(zsetKey, member);
                RankingSnapshot snapshot = new RankingSnapshot();
                snapshot.setRankType(rankType);
                snapshot.setSnapshotDate(now);
                snapshot.setRobotId(Long.valueOf(member));
                snapshot.setHotScore(score == null ? 0L : score.longValue());
                snapshot.setRankNo(rank++);
                snapshot.setCreateTime(now);
                rankingSnapshotMapper.insert(snapshot);
                count++;
            }
        }
        log.info("排行榜快照完成，共 {} 条记录", count);
        return count;
    }

    /**
     * 刷新权重缓存（管理接口可调用）
     */
    public void refreshWeightCache() {
        weightCacheTime = 0; // 强制下次访问时重新加载
        ensureWeightsLoaded();
    }

    /**
     * 获取当前权重配置
     */
    public Map<String, Integer> getWeightConfig() {
        ensureWeightsLoaded();
        return new HashMap<>(weightCache);
    }

    /**
     * 热度分计算（配置化权重 + 时间衰减）
     * <p>
     * 公式: (view×Wv + favorite×Wf + comment×Wc + compare×Wcp + inquiry×Wi + score×Ws + recency×Wr) × decay
     * 衰减: decay = max(minDecay, 2^(-elapsedDays/halfLifeDays))
     */
    private long hotScore(Robot r, String rankType) {
        ensureWeightsLoaded();
        Map<String, Integer> w = weightCache;

        long view = r.getViewCount() == null ? 0 : r.getViewCount();
        long favorite = r.getFavoriteCount() == null ? 0 : r.getFavoriteCount();
        long comment = r.getCommentCount() == null ? 0 : r.getCommentCount();
        long compare = r.getCompareCount() == null ? 0 : r.getCompareCount();
        long inquiry = r.getInquiryCount() == null ? 0 : r.getInquiryCount();
        long rating = r.getScore() == null ? 0 : r.getScore().longValue();

        // 新品加权
        int newProductWeight = w.getOrDefault("NEW_PRODUCT", 2);
        long recency = 0;
        if (r.getReleaseDate() != null) {
            long days = ChronoUnit.DAYS.between(r.getReleaseDate(), LocalDate.now());
            recency = Math.max(0, 365 - days) * newProductWeight;
        }

        long rawScore = view * w.getOrDefault("VIEW", 1)
                + favorite * w.getOrDefault("FAVORITE", 8)
                + comment * w.getOrDefault("COMMENT", 5)
                + compare * w.getOrDefault("COMPARE", 12)
                + inquiry * w.getOrDefault("INQUIRY", 30)
                + rating * w.getOrDefault("SCORE", 10)
                + recency;

        // 时间衰减
        double decay = calcDecay(r, rankType);
        return (long) (rawScore * decay);
    }

    /**
     * 计算时间衰减系数
     * decay = max(minDecay, 2^(-elapsedDays/halfLifeDays))
     */
    private double calcDecay(Robot r, String rankType) {
        if (r.getReleaseDate() == null) {
            return 1.0; // 无发布日期不衰减
        }
        long elapsedDays = ChronoUnit.DAYS.between(r.getReleaseDate(), LocalDate.now());
        if (elapsedDays <= 0) {
            return 1.0; // 未来日期不衰减
        }

        RankingDecayConfig config = decayCache.get(rankType);
        int halfLifeDays = config != null && config.getHalfLifeDays() != null
                ? config.getHalfLifeDays() : DEFAULT_HALF_LIFE_DAYS;
        double minDecay = config != null && config.getMinDecayFactor() != null
                ? config.getMinDecayFactor().doubleValue() : DEFAULT_MIN_DECAY;

        double decay = Math.pow(2, -(double) elapsedDays / halfLifeDays);
        return Math.max(minDecay, decay);
    }

    /**
     * 确保权重和衰减配置已加载到内存
     */
    private void ensureWeightsLoaded() {
        long now = System.currentTimeMillis();
        if (now - weightCacheTime < WEIGHT_CACHE_TTL_MS) {
            return; // 缓存未过期
        }
        try {
            // 加载权重
            List<RankingWeight> weights = rankingWeightMapper.selectList(null);
            if (!weights.isEmpty()) {
                Map<String, Integer> newWeightCache = new HashMap<>();
                for (RankingWeight w : weights) {
                    newWeightCache.put(w.getEventType(), w.getWeight());
                }
                weightCache = newWeightCache;
            }

            // 加载衰减配置
            List<RankingDecayConfig> decays = rankingDecayConfigMapper.selectList(null);
            if (!decays.isEmpty()) {
                Map<String, RankingDecayConfig> newDecayCache = new HashMap<>();
                for (RankingDecayConfig d : decays) {
                    newDecayCache.put(d.getRankType(), d);
                }
                decayCache = newDecayCache;
            }

            weightCacheTime = now;
            log.info("排行榜配置已加载: weights={}, decay={}", weightCache.keySet(), decayCache.keySet());
        } catch (Exception e) {
            log.warn("加载排行榜配置失败，使用默认值: {}", e.getMessage());
            weightCache = new HashMap<>(DEFAULT_WEIGHTS);
            weightCacheTime = now; // 避免频繁重试
        }
    }

    private String joinIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }

    private List<Long> parseIds(String cached) {
        List<Long> ids = new ArrayList<>();
        if (StrUtil.isBlank(cached)) {
            return ids;
        }
        for (String part : cached.split(",")) {
            if (StrUtil.isNotBlank(part)) {
                try {
                    ids.add(Long.valueOf(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }
}