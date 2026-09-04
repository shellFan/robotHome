package com.robot.home.ranking.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.ranking.service.RankingService;
import com.robot.home.ranking.vo.RankingTypeVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotCategory;
import com.robot.home.robot.mapper.RobotCategoryMapper;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.service.RobotService;
import com.robot.home.robot.vo.RobotListVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 排行榜实现
 * <p>
 * 热度权重算法（weight = 行为权重 × 行为次数 + 时间衰减）：
 *  浏览量 × 1 + 收藏 × 5 + 评论 × 8 + 对比 × 6 + 询价 × 15 + 评分(0-10) × 100
 *  + 新品加权：max(0, 365 - 距发布天数) × 2
 * 说明：询价是强购买意向，权重最高；浏览是最弱信号，权重最低。
 */
@Service
public class RankingServiceImpl implements RankingService {

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

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotCategoryMapper categoryMapper;
    @Resource
    private RobotService robotService;
    @Resource
    private RedisUtils redisUtils;

    @Value("${robot.ranking-cache:600}")
    private long rankingCacheSeconds;

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
            // 在 Java 中统一计算热度分，保证与定时任务落库的 hot_score 使用同一套算法
            robots.sort((a, b) -> Long.compare(hotScore(b), hotScore(a)));
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
        // selectListByIds 不保证顺序，这里按榜单顺序还原
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
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery().orderByAsc(Robot::getId));
        int updated = 0;
        for (Robot r : robots) {
            long score = hotScore(r);
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
     * 榜单 id 列表序列化为逗号分隔字符串（避免复杂类型反序列化的类型丢失问题）
     */
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
                    // 忽略异常数据
                }
            }
        }
        return ids;
    }

    /**
     * 热度分计算：浏览量×1 + 收藏×5 + 评论×8 + 对比×6 + 询价×15 + 评分×100 + 新品加权
     */
    private long hotScore(Robot r) {
        long view = r.getViewCount() == null ? 0 : r.getViewCount();
        long favorite = r.getFavoriteCount() == null ? 0 : r.getFavoriteCount();
        long comment = r.getCommentCount() == null ? 0 : r.getCommentCount();
        long compare = r.getCompareCount() == null ? 0 : r.getCompareCount();
        long inquiry = r.getInquiryCount() == null ? 0 : r.getInquiryCount();
        long rating = r.getScore() == null ? 0 : r.getScore().longValue();
        long recency = 0;
        if (r.getReleaseDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(r.getReleaseDate(), java.time.LocalDate.now());
            recency = Math.max(0, 365 - days) * 2;
        }
        return view + favorite * 5 + comment * 8 + compare * 6 + inquiry * 15 + rating * 100 + recency;
    }
}
