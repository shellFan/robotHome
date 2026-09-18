package com.robot.home.ranking.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.ranking.entity.RankingDecayConfig;
import com.robot.home.ranking.entity.RankingSnapshot;
import com.robot.home.ranking.entity.RankingWeight;
import com.robot.home.ranking.mapper.RankingDecayConfigMapper;
import com.robot.home.ranking.mapper.RankingSnapshotMapper;
import com.robot.home.ranking.mapper.RankingWeightMapper;
import com.robot.home.ranking.service.RankingService;
import com.robot.home.ranking.vo.RankingSnapshotVO;
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
    @Resource
    private BrandMapper brandMapper;

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
    public List<RobotListVO> rank(String type, int limit, Long currentUserId, String timeRange) {
        String rankType = StrUtil.isBlank(type) ? Constants.RANK_HOT : type;
        String range = StrUtil.isBlank(timeRange) ? "all" : timeRange.toLowerCase();
        int size = Math.max(1, Math.min(limit, 100));

        // Phase9: 新榜单类型走专用排序
        if (isNewRankType(rankType)) {
            return rankByNewType(rankType, size, currentUserId);
        }

        String cacheKey = Constants.CACHE_RANKING_PREFIX + rankType + ":" + range + ":" + size;
        List<Long> ids = null;
        try {
            ids = parseIds(redisUtils.get(cacheKey));
        } catch (Exception e) {
            log.warn("Redis排行榜缓存读取失败，降级到DB查询: rankType={}, error={}", rankType, e.getMessage());
        }
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
            // 时间范围过滤
            LocalDate dateFrom = calcDateFrom(range);
            List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                    .in(categoryIds != null && !categoryIds.isEmpty(), Robot::getCategoryId, categoryIds)
                    .ge(dateFrom != null, Robot::getReleaseDate, dateFrom));
            // 使用配置化权重 + 时间衰减计算热度
            robots.sort((a, b) -> Long.compare(hotScore(b, rankType), hotScore(a, rankType)));
            ids = new ArrayList<>();
            for (int i = 0; i < robots.size() && ids.size() < size; i++) {
                ids.add(robots.get(i).getId());
            }
            if (!ids.isEmpty()) {
                try {
                    redisUtils.set(cacheKey, joinIds(ids), rankingCacheSeconds, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Redis排行榜缓存写入失败（不影响返回）: rankType={}, error={}", rankType, e.getMessage());
                }
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
                new RankingTypeVO(Constants.RANK_DEV, "开发机器人榜"),
                // Phase9 新增榜单
                new RankingTypeVO(Constants.RANK_FOLLOW, "关注榜"),
                new RankingTypeVO(Constants.RANK_FAVORITE, "收藏榜"),
                new RankingTypeVO(Constants.RANK_DISCUSSION, "讨论榜"),
                new RankingTypeVO(Constants.RANK_REVIEW, "口碑榜"),
                new RankingTypeVO(Constants.RANK_NEW_PRODUCT, "新品榜"),
                new RankingTypeVO(Constants.RANK_COMPANY_ATTENTION, "企业关注榜"));
    }

    @Override
    public int refreshHotScores() {
        ensureWeightsLoaded();
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery().orderByAsc(Robot::getId));
        int updated = 0;
        // 批量更新hotScore，避免N+1逐条UPDATE
        List<Robot> batch = new ArrayList<>();
        int batchSize = 50;
        for (Robot r : robots) {
            long score = hotScore(r, Constants.RANK_HOT);
            Robot update = new Robot();
            update.setId(r.getId());
            update.setHotScore(score);
            batch.add(update);
            if (batch.size() >= batchSize) {
                for (Robot u : batch) {
                    robotMapper.updateById(u);
                }
                updated += batch.size();
                batch.clear();
            }
        }
        // 处理剩余
        for (Robot u : batch) {
            robotMapper.updateById(u);
        }
        updated += batch.size();
        // 刷新后清空榜单缓存
        try {
            for (String key : new ArrayList<>(redisUtils.keys(Constants.CACHE_RANKING_PREFIX + "*"))) {
                redisUtils.delete(key);
            }
        } catch (Exception e) {
            log.warn("Redis排行榜缓存清空失败: error={}", e.getMessage());
        }
        return updated;
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

    /**
     * 根据时间范围计算起始日期
     * @param range day/week/month/all
     * @return 起始日期，all返回null
     */
    private LocalDate calcDateFrom(String range) {
        LocalDate now = LocalDate.now();
        switch (range) {
            case "day":
                return now.minusDays(1);
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            default:
                return null; // all: 不限制时间
        }
    }

    // ========== Phase9 新增方法 ==========

    /** Phase9新榜单类型 */
    private boolean isNewRankType(String rankType) {
        return Constants.RANK_FOLLOW.equals(rankType)
                || Constants.RANK_FAVORITE.equals(rankType)
                || Constants.RANK_DISCUSSION.equals(rankType)
                || Constants.RANK_REVIEW.equals(rankType)
                || Constants.RANK_NEW_PRODUCT.equals(rankType)
                || Constants.RANK_COMPANY_ATTENTION.equals(rankType);
    }

    /** Phase9: 新榜单类型排序 */
    private List<RobotListVO> rankByNewType(String rankType, int limit, Long currentUserId) {
        String cacheKey = Constants.CACHE_RANKING_PREFIX + rankType + ":all:" + limit;
        List<Long> ids = null;
        try {
            ids = parseIds(redisUtils.get(cacheKey));
        } catch (Exception e) {
            log.warn("Redis新榜单缓存读取失败: rankType={}, error={}", rankType, e.getMessage());
        }
        if (ids == null || ids.isEmpty()) {
            List<Robot> robots;
            switch (rankType) {
                case Constants.RANK_FOLLOW:
                    robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                            .eq(Robot::getStatus, 1)
                            .gt(Robot::getFollowCount, 0)
                            .orderByDesc(Robot::getFollowCount)
                            .last("LIMIT " + limit));
                    break;
                case Constants.RANK_FAVORITE:
                    robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                            .eq(Robot::getStatus, 1)
                            .gt(Robot::getFavoriteCount, 0)
                            .orderByDesc(Robot::getFavoriteCount)
                            .last("LIMIT " + limit));
                    break;
                case Constants.RANK_DISCUSSION:
                    robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                            .eq(Robot::getStatus, 1)
                            .gt(Robot::getDiscussionCount, 0)
                            .orderByDesc(Robot::getDiscussionCount)
                            .last("LIMIT " + limit));
                    break;
                case Constants.RANK_REVIEW:
                    robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                            .eq(Robot::getStatus, 1)
                            .gt(Robot::getReviewCount, 0)
                            .orderByDesc(Robot::getReviewCount)
                            .last("LIMIT " + limit));
                    break;
                case Constants.RANK_NEW_PRODUCT:
                    robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                            .eq(Robot::getStatus, 1)
                            .isNotNull(Robot::getReleaseDate)
                            .orderByDesc(Robot::getReleaseDate)
                            .last("LIMIT " + limit));
                    break;
                case Constants.RANK_COMPANY_ATTENTION:
                    // 企业关注榜：按品牌所属企业的关注热度排序
                    // 简化实现：按hotScore排序（企业维度暂用品牌维度聚合）
                    robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                            .eq(Robot::getStatus, 1)
                            .gt(Robot::getHotScore, 0L)
                            .orderByDesc(Robot::getHotScore)
                            .last("LIMIT " + limit));
                    break;
                default:
                    robots = new ArrayList<>();
            }
            ids = new ArrayList<>();
            for (Robot r : robots) {
                ids.add(r.getId());
            }
            if (!ids.isEmpty()) {
                try {
                    redisUtils.set(cacheKey, joinIds(ids), rankingCacheSeconds, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Redis新榜单缓存写入失败: rankType={}, error={}", rankType, e.getMessage());
                }
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
    public int snapshotRankings() {
        String[] rankTypes = {Constants.RANK_HOT, Constants.RANK_HUMANOID, Constants.RANK_QUADRUPED,
                Constants.RANK_SERVICE, Constants.RANK_INDUSTRIAL, Constants.RANK_FAMILY, Constants.RANK_DEV,
                Constants.RANK_FOLLOW, Constants.RANK_FAVORITE, Constants.RANK_DISCUSSION,
                Constants.RANK_REVIEW, Constants.RANK_NEW_PRODUCT, Constants.RANK_COMPANY_ATTENTION};
        Date now = new Date();
        int count = 0;
        for (String rankType : rankTypes) {
            // Phase9: 读取前一天的快照用于计算排名变化
            Map<Long, Integer> prevRanks = loadPrevRanks(rankType);

            String zsetKey = Constants.CACHE_HOT_PREFIX + rankType;
            Set<String> members = null;
            try {
                members = redisUtils.zReverseRange(zsetKey, 0, 99);
            } catch (Exception e) {
                log.warn("Redis ZSET读取失败，跳过排行榜快照: rankType={}, error={}", rankType, e.getMessage());
                // Phase9: 新榜单类型从DB fallback
                if (isNewRankType(rankType)) {
                    count += snapshotFromDB(rankType, now, prevRanks, 100);
                }
                continue;
            }
            if (members == null || members.isEmpty()) {
                // Phase9: 新榜单类型从DB fallback
                if (isNewRankType(rankType)) {
                    count += snapshotFromDB(rankType, now, prevRanks, 100);
                }
                continue;
            }
            int rank = 1;
            for (String member : members) {
                Double score = null;
                try {
                    score = redisUtils.zScore(zsetKey, member);
                } catch (Exception e) {
                    log.warn("Redis ZSET分数读取失败: rankType={}, member={}, error={}", rankType, member, e.getMessage());
                }
                RankingSnapshot snapshot = new RankingSnapshot();
                snapshot.setRankType(rankType);
                snapshot.setSnapshotDate(now);
                snapshot.setRobotId(Long.valueOf(member));
                snapshot.setHotScore(score == null ? 0L : score.longValue());
                snapshot.setRankNo(rank);
                // Phase9: 计算排名变化
                Integer prevRank = prevRanks.get(Long.valueOf(member));
                snapshot.setPrevRankNo(prevRank);
                if (prevRank == null) {
                    snapshot.setRankChange(null); // 新上榜
                } else {
                    snapshot.setRankChange(prevRank - rank); // 正数上升，负数下降
                }
                // Phase9: 生成上榜原因
                String[] reason = generateReason(rankType, rank, prevRank, score);
                snapshot.setReasonCode(reason[0]);
                snapshot.setReasonText(reason[1]);
                snapshot.setCreateTime(now);
                rankingSnapshotMapper.insert(snapshot);
                rank++;
                count++;
            }
        }
        log.info("排行榜快照完成，共 {} 条记录", count);
        return count;
    }

    /** Phase9: 从DB生成新榜单类型快照 */
    private int snapshotFromDB(String rankType, Date now, Map<Long, Integer> prevRanks, int limit) {
        List<Robot> robots;
        switch (rankType) {
            case Constants.RANK_FOLLOW:
                robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                        .eq(Robot::getStatus, 1).gt(Robot::getFollowCount, 0)
                        .orderByDesc(Robot::getFollowCount).last("LIMIT " + limit));
                break;
            case Constants.RANK_FAVORITE:
                robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                        .eq(Robot::getStatus, 1).gt(Robot::getFavoriteCount, 0)
                        .orderByDesc(Robot::getFavoriteCount).last("LIMIT " + limit));
                break;
            case Constants.RANK_DISCUSSION:
                robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                        .eq(Robot::getStatus, 1).gt(Robot::getDiscussionCount, 0)
                        .orderByDesc(Robot::getDiscussionCount).last("LIMIT " + limit));
                break;
            case Constants.RANK_REVIEW:
                robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                        .eq(Robot::getStatus, 1).gt(Robot::getReviewCount, 0)
                        .orderByDesc(Robot::getReviewCount).last("LIMIT " + limit));
                break;
            case Constants.RANK_NEW_PRODUCT:
                robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                        .eq(Robot::getStatus, 1).isNotNull(Robot::getReleaseDate)
                        .orderByDesc(Robot::getReleaseDate).last("LIMIT " + limit));
                break;
            default:
                robots = new ArrayList<>();
        }
        int count = 0;
        int rank = 1;
        for (Robot r : robots) {
            RankingSnapshot snapshot = new RankingSnapshot();
            snapshot.setRankType(rankType);
            snapshot.setSnapshotDate(now);
            snapshot.setRobotId(r.getId());
            snapshot.setHotScore(r.getHotScore() == null ? 0L : r.getHotScore());
            snapshot.setRankNo(rank);
            Integer prevRank = prevRanks.get(r.getId());
            snapshot.setPrevRankNo(prevRank);
            snapshot.setRankChange(prevRank == null ? null : prevRank - rank);
            String[] reason = generateReason(rankType, rank, prevRank, (double) snapshot.getHotScore());
            snapshot.setReasonCode(reason[0]);
            snapshot.setReasonText(reason[1]);
            snapshot.setCreateTime(now);
            rankingSnapshotMapper.insert(snapshot);
            rank++;
            count++;
        }
        return count;
    }

    /** Phase9: 读取前一天排名 */
    private Map<Long, Integer> loadPrevRanks(String rankType) {
        Map<Long, Integer> prevRanks = new HashMap<>();
        try {
            // 查询最近一天的快照（不是今天的）
            RankingSnapshot latest = rankingSnapshotMapper.selectOne(Wrappers.<RankingSnapshot>lambdaQuery()
                    .eq(RankingSnapshot::getRankType, rankType)
                    .orderByDesc(RankingSnapshot::getSnapshotDate)
                    .last("LIMIT 1"));
            if (latest != null) {
                // 查询该天的所有排名
                List<RankingSnapshot> prevList = rankingSnapshotMapper.selectList(Wrappers.<RankingSnapshot>lambdaQuery()
                        .eq(RankingSnapshot::getRankType, rankType)
                        .eq(RankingSnapshot::getSnapshotDate, latest.getSnapshotDate()));
                for (RankingSnapshot s : prevList) {
                    prevRanks.put(s.getRobotId(), s.getRankNo());
                }
            }
        } catch (Exception e) {
            log.warn("读取前日排名失败: rankType={}, error={}", rankType, e.getMessage());
        }
        return prevRanks;
    }

    /** Phase9: 生成上榜原因 */
    private String[] generateReason(String rankType, int currentRank, Integer prevRank, Double score) {
        if (prevRank == null) {
            // 新上榜
            if (Constants.RANK_NEW_PRODUCT.equals(rankType)) {
                return new String[]{Constants.REASON_NEW_PRODUCT, "近期新发布机器人"};
            }
            return new String[]{Constants.REASON_NEW_ENTRY, "新上榜"};
        }
        int change = prevRank - currentRank;
        if (change > 5) {
            return new String[]{Constants.REASON_HOT_RISE, "排名大幅上升"};
        }
        if (change > 0) {
            return new String[]{Constants.REASON_HOT_RISE, "排名上升" + change + "位"};
        }
        if (Constants.RANK_FAVORITE.equals(rankType)) {
            return new String[]{Constants.REASON_FAVORITE_BOOST, "收藏热度较高"};
        }
        if (Constants.RANK_DISCUSSION.equals(rankType)) {
            return new String[]{Constants.REASON_DISCUSSION_ACTIVE, "近期讨论活跃"};
        }
        if (Constants.RANK_REVIEW.equals(rankType)) {
            return new String[]{Constants.REASON_REVIEW_POSITIVE, "用户评价较好"};
        }
        if (Constants.RANK_FOLLOW.equals(rankType)) {
            return new String[]{Constants.REASON_FOLLOW_GROWTH, "关注增长较快"};
        }
        if (Constants.RANK_NEW_PRODUCT.equals(rankType)) {
            return new String[]{Constants.REASON_NEW_PRODUCT, "近期新发布"};
        }
        return new String[]{Constants.REASON_SCORE_UP, "综合评分较高"};
    }

    @Override
    public List<RankingSnapshotVO> rankWithChange(String type, int limit) {
        String rankType = StrUtil.isBlank(type) ? Constants.RANK_HOT : type;
        int size = Math.max(1, Math.min(limit, 100));

        // 白名单校验
        if (!isValidRankType(rankType)) {
            return new ArrayList<>();
        }

        // 读取最新快照
        RankingSnapshot latest = rankingSnapshotMapper.selectOne(Wrappers.<RankingSnapshot>lambdaQuery()
                .eq(RankingSnapshot::getRankType, rankType)
                .orderByDesc(RankingSnapshot::getSnapshotDate)
                .last("LIMIT 1"));
        if (latest == null) {
            return new ArrayList<>();
        }

        List<RankingSnapshot> snapshots = rankingSnapshotMapper.selectList(Wrappers.<RankingSnapshot>lambdaQuery()
                .eq(RankingSnapshot::getRankType, rankType)
                .eq(RankingSnapshot::getSnapshotDate, latest.getSnapshotDate())
                .orderByAsc(RankingSnapshot::getRankNo)
                .last("LIMIT " + size));
        if (snapshots.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量加载Robot
        List<Long> robotIds = new ArrayList<>();
        for (RankingSnapshot s : snapshots) {
            robotIds.add(s.getRobotId());
        }
        List<Robot> robots = robotMapper.selectBatchIds(robotIds);
        Map<Long, Robot> robotMap = new HashMap<>();
        for (Robot r : robots) {
            robotMap.put(r.getId(), r);
        }

        // 批量加载Brand
        Set<Long> brandIds = new java.util.HashSet<>();
        for (Robot r : robots) {
            if (r.getBrandId() != null) {
                brandIds.add(r.getBrandId());
            }
        }
        Map<Long, String> brandNameMap = new HashMap<>();
        if (!brandIds.isEmpty()) {
            List<com.robot.home.brand.entity.Brand> brands = brandMapper.selectBatchIds(brandIds);
            for (com.robot.home.brand.entity.Brand b : brands) {
                brandNameMap.put(b.getId(), b.getName());
            }
        }

        List<RankingSnapshotVO> result = new ArrayList<>();
        for (RankingSnapshot s : snapshots) {
            Robot r = robotMap.get(s.getRobotId());
            if (r == null) {
                continue;
            }
            RankingSnapshotVO vo = new RankingSnapshotVO();
            vo.setId(s.getId());
            vo.setRankType(s.getRankType());
            vo.setSnapshotDate(s.getCreateTime() != null ? java.time.LocalDateTime.ofInstant(s.getCreateTime().toInstant(), java.time.ZoneId.systemDefault()) : null);
            vo.setRobotId(s.getRobotId());
            vo.setRobotName(r.getName());
            vo.setRobotCoverImage(r.getCoverImage());
            vo.setRobotSubtitle(r.getSubtitle());
            vo.setBrandId(r.getBrandId());
            vo.setBrandName(r.getBrandId() != null ? brandNameMap.get(r.getBrandId()) : null);
            vo.setGuidePrice(r.getGuidePrice());
            vo.setHotScore(s.getHotScore());
            vo.setRankNo(s.getRankNo());
            vo.setPrevRankNo(s.getPrevRankNo());
            vo.setRankChange(s.getRankChange());
            vo.setReasonCode(s.getReasonCode());
            vo.setReasonText(s.getReasonText());
            vo.setScore(r.getScore());
            vo.setFavoriteCount(r.getFavoriteCount());
            vo.setDiscussionCount(r.getDiscussionCount());
            vo.setFollowCount(r.getFollowCount());
            result.add(vo);
        }
        return result;
    }

    /** 榜单类型白名单校验 */
    private boolean isValidRankType(String type) {
        if (StrUtil.isBlank(type)) {
            return false;
        }
        String[] validTypes = {
                Constants.RANK_HOT, Constants.RANK_HUMANOID, Constants.RANK_QUADRUPED,
                Constants.RANK_SERVICE, Constants.RANK_INDUSTRIAL, Constants.RANK_FAMILY, Constants.RANK_DEV,
                Constants.RANK_FOLLOW, Constants.RANK_FAVORITE, Constants.RANK_DISCUSSION,
                Constants.RANK_REVIEW, Constants.RANK_NEW_PRODUCT, Constants.RANK_COMPANY_ATTENTION
        };
        for (String valid : validTypes) {
            if (valid.equals(type)) {
                return true;
            }
        }
        return false;
    }
}