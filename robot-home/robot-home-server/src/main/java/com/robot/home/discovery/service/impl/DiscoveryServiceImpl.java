package com.robot.home.discovery.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.brand.vo.BrandListVO;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.community.vo.PostVO;
import com.robot.home.discovery.service.DiscoveryService;
import com.robot.home.discovery.vo.DiscoveryHomeVO;
import com.robot.home.discovery.vo.DiscoveryRankItemVO;
import com.robot.home.discovery.vo.DiscoveryRankingCardVO;
import com.robot.home.qa.entity.RobotQuestion;
import com.robot.home.qa.mapper.RobotQuestionMapper;
import com.robot.home.qa.vo.QuestionVO;
import com.robot.home.ranking.entity.RankingSnapshot;
import com.robot.home.ranking.mapper.RankingSnapshotMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.service.RobotService;
import com.robot.home.robot.vo.RobotListVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 发现页服务实现
 * <p>
 * 聚合多维度数据：热门/新品/高评分/高收藏/高讨论/品牌/分类/榜单/讨论/问答
 * 使用Redis缓存 + DB fallback
 * 批量查询避免N+1
 */
@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryServiceImpl.class);

    private static final long CACHE_SECONDS = 180L;
    private static final int DEFAULT_LIMIT = 10;

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotService robotService;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private CommunityPostMapper communityPostMapper;
    @Resource
    private RobotQuestionMapper robotQuestionMapper;
    @Resource
    private RankingSnapshotMapper rankingSnapshotMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public DiscoveryHomeVO home(Long currentUserId, String position) {
        boolean isPc = "pc".equalsIgnoreCase(position);
        int limit = isPc ? 15 : DEFAULT_LIMIT;
        String cacheKey = "robot:discovery:home:" + (isPc ? "pc" : "miniapp");
        try {
            DiscoveryHomeVO cached = redisUtils.getObj(cacheKey, DiscoveryHomeVO.class);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis发现页缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }

        DiscoveryHomeVO vo = new DiscoveryHomeVO();
        vo.setHotRobots(hotRobots(limit, currentUserId));
        vo.setTrendingRobots(trendingRobots(limit, currentUserId));
        vo.setNewRobots(newRobots(limit, currentUserId));
        vo.setTopRatedRobots(topRatedRobots(limit, currentUserId));
        vo.setMostFavoritedRobots(mostFavoritedRobots(limit, currentUserId));
        vo.setMostDiscussedRobots(mostDiscussedRobots(limit, currentUserId));
        vo.setHotBrands(hotBrands(limit));
        vo.setCategories(robotService.categoryTree());
        vo.setRankingCards(buildRankingCards());
        vo.setHotPosts(hotPosts(isPc ? 8 : 5));
        vo.setHotQuestions(hotQuestions(isPc ? 8 : 5));

        try {
            redisUtils.setObj(cacheKey, vo, CACHE_SECONDS + (long) (Math.random() * 30), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis发现页缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
        return vo;
    }

    @Override
    public List<RobotListVO> hotRobots(int limit, Long currentUserId) {
        int size = Math.max(1, Math.min(limit, 50));
        String cacheKey = "robot:discovery:hot:" + size;
        List<Long> ids = getCachedIds(cacheKey);
        if (ids == null) {
            List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                    .eq(Robot::getStatus, 1)
                    .orderByDesc(Robot::getHotScore)
                    .last("LIMIT " + size));
            ids = robots.stream().map(Robot::getId).collect(Collectors.toList());
            setCachedIds(cacheKey, ids);
        }
        return loadRobotListVOs(ids, currentUserId);
    }

    @Override
    public List<RobotListVO> trendingRobots(int limit, Long currentUserId) {
        int size = Math.max(1, Math.min(limit, 50));
        String cacheKey = "robot:discovery:trending:" + size;
        List<Long> ids = getCachedIds(cacheKey);
        if (ids == null) {
            // 近7天行为热度（从Redis ZSET读取）
            ids = getTrendingFromRedis(size);
            if (ids.isEmpty()) {
                // fallback: 按hotScore降序
                List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                        .eq(Robot::getStatus, 1)
                        .orderByDesc(Robot::getHotScore)
                        .last("LIMIT " + size));
                ids = robots.stream().map(Robot::getId).collect(Collectors.toList());
            }
            setCachedIds(cacheKey, ids);
        }
        return loadRobotListVOs(ids, currentUserId);
    }

    @Override
    public List<RobotListVO> newRobots(int limit, Long currentUserId) {
        int size = Math.max(1, Math.min(limit, 50));
        String cacheKey = "robot:discovery:new:" + size;
        List<Long> ids = getCachedIds(cacheKey);
        if (ids == null) {
            List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                    .eq(Robot::getStatus, 1)
                    .isNotNull(Robot::getReleaseDate)
                    .orderByDesc(Robot::getReleaseDate)
                    .last("LIMIT " + size));
            ids = robots.stream().map(Robot::getId).collect(Collectors.toList());
            setCachedIds(cacheKey, ids);
        }
        return loadRobotListVOs(ids, currentUserId);
    }

    @Override
    public List<DiscoveryRankItemVO> rankRobots(String rankType, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        String type = StrUtil.isBlank(rankType) ? Constants.RANK_HOT : rankType;
        // 白名单校验
        if (!isValidRankType(type)) {
            return Collections.emptyList();
        }
        // 从快照表读取最新一天的数据
        // 先取最新快照日期
        RankingSnapshot latest = rankingSnapshotMapper.selectOne(Wrappers.<RankingSnapshot>lambdaQuery()
                .eq(RankingSnapshot::getRankType, type)
                .orderByDesc(RankingSnapshot::getSnapshotDate)
                .last("LIMIT 1"));
        if (latest == null) {
            return Collections.emptyList();
        }
        // 按该日期取全部排名
        List<RankingSnapshot> snapshots = rankingSnapshotMapper.selectList(Wrappers.<RankingSnapshot>lambdaQuery()
                .eq(RankingSnapshot::getRankType, type)
                .eq(RankingSnapshot::getSnapshotDate, latest.getSnapshotDate())
                .orderByAsc(RankingSnapshot::getRankNo)
                .last("LIMIT " + size));
        if (snapshots.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量加载Robot
        List<Long> robotIds = snapshots.stream().map(RankingSnapshot::getRobotId).collect(Collectors.toList());
        List<Robot> robots = robotMapper.selectBatchIds(robotIds);
        Map<Long, Robot> robotMap = new HashMap<>();
        for (Robot r : robots) {
            robotMap.put(r.getId(), r);
        }
        // 批量加载Brand
        Set<Long> brandIds = robots.stream().map(Robot::getBrandId).filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, Brand> brandMap = new HashMap<>();
        if (!brandIds.isEmpty()) {
            brandMapper.selectBatchIds(brandIds).forEach(b -> brandMap.put(b.getId(), b));
        }
        List<DiscoveryRankItemVO> items = new ArrayList<>();
        for (RankingSnapshot s : snapshots) {
            Robot r = robotMap.get(s.getRobotId());
            if (r == null) {
                continue;
            }
            DiscoveryRankItemVO item = new DiscoveryRankItemVO();
            item.setRobotId(r.getId());
            item.setName(r.getName());
            item.setCoverImage(r.getCoverImage());
            item.setSubtitle(r.getSubtitle());
            item.setBrandId(r.getBrandId());
            item.setGuidePrice(r.getGuidePrice());
            item.setHotScore(s.getHotScore());
            item.setRankNo(s.getRankNo());
            item.setRankChange(s.getRankChange());
            item.setReasonCode(s.getReasonCode());
            item.setReasonText(s.getReasonText());
            if (r.getBrandId() != null) {
                Brand b = brandMap.get(r.getBrandId());
                if (b != null) {
                    item.setBrandName(b.getName());
                }
            }
            items.add(item);
        }
        return items;
    }

    // ========== 内部方法 ==========

    private List<RobotListVO> topRatedRobots(int limit, Long currentUserId) {
        int size = Math.max(1, Math.min(limit, 50));
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getStatus, 1)
                .gt(Robot::getScore, 0)
                .orderByDesc(Robot::getScore)
                .last("LIMIT " + size));
        List<Long> ids = robots.stream().map(Robot::getId).collect(Collectors.toList());
        return loadRobotListVOs(ids, currentUserId);
    }

    private List<RobotListVO> mostFavoritedRobots(int limit, Long currentUserId) {
        int size = Math.max(1, Math.min(limit, 50));
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getStatus, 1)
                .gt(Robot::getFavoriteCount, 0)
                .orderByDesc(Robot::getFavoriteCount)
                .last("LIMIT " + size));
        List<Long> ids = robots.stream().map(Robot::getId).collect(Collectors.toList());
        return loadRobotListVOs(ids, currentUserId);
    }

    private List<RobotListVO> mostDiscussedRobots(int limit, Long currentUserId) {
        int size = Math.max(1, Math.min(limit, 50));
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getStatus, 1)
                .gt(Robot::getDiscussionCount, 0)
                .orderByDesc(Robot::getDiscussionCount)
                .last("LIMIT " + size));
        List<Long> ids = robots.stream().map(Robot::getId).collect(Collectors.toList());
        return loadRobotListVOs(ids, currentUserId);
    }

    private List<BrandListVO> hotBrands(int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        List<Brand> brands = brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByDesc(Brand::getHotScore)
                .last("LIMIT " + size));
        List<BrandListVO> result = new ArrayList<>();
        for (Brand b : brands) {
            BrandListVO vo = new BrandListVO();
            vo.setId(b.getId());
            vo.setName(b.getName());
            vo.setLogo(b.getLogo());
            vo.setIntro(b.getIntro());
            vo.setCountry(b.getCountry());
            vo.setRobotCount(b.getRobotCount());
            vo.setFollowCount(b.getFollowCount());
            result.add(vo);
        }
        return result;
    }

    private List<DiscoveryRankingCardVO> buildRankingCards() {
        List<DiscoveryRankingCardVO> cards = new ArrayList<>();
        String[][] types = {
                {Constants.RANK_HOT, "热门榜", "综合热度排名"},
                {Constants.RANK_HUMANOID, "人形机器人", "人形机器人热门排名"},
                {Constants.RANK_QUADRUPED, "机器狗", "四足机器人热门排名"},
                {"new_product", "新品榜", "近期新发布机器人"},
                {"review", "口碑榜", "用户评分最高"}
        };
        for (String[] t : types) {
            DiscoveryRankingCardVO card = new DiscoveryRankingCardVO();
            card.setRankType(t[0]);
            card.setRankName(t[1]);
            card.setDescription(t[2]);
            card.setItems(rankRobots(t[0], 5));
            cards.add(card);
        }
        return cards;
    }

    private List<PostVO> hotPosts(int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        List<CommunityPost> posts = communityPostMapper.selectList(Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getStatus, 1)
                .orderByDesc(CommunityPost::getHotScore)
                .last("LIMIT " + size));
        List<PostVO> result = new ArrayList<>();
        for (CommunityPost p : posts) {
            PostVO vo = new PostVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setViewCount(p.getViewCount());
            vo.setLikeCount(p.getLikeCount());
            vo.setCommentCount(p.getCommentCount());
            vo.setCreateTime(p.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private List<QuestionVO> hotQuestions(int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        List<RobotQuestion> questions = robotQuestionMapper.selectList(Wrappers.<RobotQuestion>lambdaQuery()
                .eq(RobotQuestion::getStatus, 1)
                .orderByDesc(RobotQuestion::getViewCount)
                .last("LIMIT " + size));
        List<QuestionVO> result = new ArrayList<>();
        for (RobotQuestion q : questions) {
            QuestionVO vo = new QuestionVO();
            vo.setId(q.getId());
            vo.setTitle(q.getTitle());
            vo.setAnswerCount(q.getAnswerCount());
            vo.setViewCount(q.getViewCount());
            vo.setFollowCount(q.getFollowCount());
            vo.setHasAccepted(q.getHasAccepted());
            vo.setCreateTime(q.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private List<Long> getTrendingFromRedis(int size) {
        List<Long> ids = new ArrayList<>();
        try {
            String zsetKey = Constants.CACHE_HOT_ZSET_PREFIX + "robot";
            Set<String> members = redisUtils.zReverseRange(zsetKey, 0, size - 1);
            if (members != null) {
                for (String m : members) {
                    try {
                        ids.add(Long.valueOf(m));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Redis ZSET读取trending失败，降级到DB: error={}", e.getMessage());
        }
        return ids;
    }

    private List<RobotListVO> loadRobotListVOs(List<Long> ids, Long currentUserId) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<RobotListVO> list = robotMapper.selectListByIds(ids);
        // 保持顺序
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

    private List<Long> getCachedIds(String cacheKey) {
        try {
            String cached = redisUtils.get(cacheKey);
            if (StrUtil.isNotBlank(cached)) {
                List<Long> ids = new ArrayList<>();
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
        } catch (Exception e) {
            log.warn("Redis缓存读取失败: key={}, error={}", cacheKey, e.getMessage());
        }
        return null;
    }

    private void setCachedIds(String cacheKey, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (Long id : ids) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(id);
            }
            long ttl = CACHE_SECONDS + (long) (Math.random() * 30);
            redisUtils.set(cacheKey, sb.toString(), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis缓存写入失败（不影响返回）: key={}, error={}", cacheKey, e.getMessage());
        }
    }

    private boolean isValidRankType(String type) {
        if (StrUtil.isBlank(type)) {
            return false;
        }
        // 白名单
        String[] validTypes = {
                Constants.RANK_HOT, Constants.RANK_HUMANOID, Constants.RANK_QUADRUPED,
                Constants.RANK_SERVICE, Constants.RANK_INDUSTRIAL, Constants.RANK_FAMILY, Constants.RANK_DEV,
                "follow", "favorite", "discussion", "review", "new_product", "company_attention"
        };
        for (String valid : validTypes) {
            if (valid.equals(type)) {
                return true;
            }
        }
        return false;
    }
}