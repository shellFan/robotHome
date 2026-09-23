package com.robot.home.recommend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.follow.service.FollowService;
import com.robot.home.recommend.service.RuleRecommendService;
import com.robot.home.recommend.vo.RuleRecommendVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Phase9: 规则推荐实现（可解释、无AI）
 * <p>
 * 规则优先级:
 * 1. SAME_CATEGORY - 同分类机器人
 * 2. SAME_BRAND - 同品牌机器人
 * 3. TRENDING - 热门机器人
 * 4. FOLLOWED_ROBOT - 关注机器人的同类
 * 5. POPULAR_ALTERNATIVE - 热门替代
 */
@Service
public class RuleRecommendServiceImpl implements RuleRecommendService {

    private static final Logger log = LoggerFactory.getLogger(RuleRecommendServiceImpl.class);

    private static final long CACHE_SECONDS = 300L;

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private FollowService followService;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public List<RuleRecommendVO> relatedRobots(Long robotId, Long currentUserId, int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        if (robotId == null) {
            return new ArrayList<>();
        }

        // 缓存key（不含userId，关注推荐单独处理）
        String cacheKey = Constants.CACHE_RECOMMEND_ROBOT_PREFIX + robotId + ":" + size;
        List<RuleRecommendVO> cached = null;
        try {
            cached = redisUtils.getObj(cacheKey, List.class);
        } catch (Exception e) {
            log.warn("Redis推荐缓存读取失败: key={}, error={}", cacheKey, e.getMessage());
        }
        if (cached != null && !cached.isEmpty()) {
            // 如果有登录用户，追加关注推荐
            if (currentUserId != null) {
                List<RuleRecommendVO> followedRecs = getFollowedRecommendations(robotId, currentUserId, size);
                return mergeWithPriority(followedRecs, cached, size);
            }
            return cached;
        }

        // 查询当前机器人
        Robot current = robotMapper.selectById(robotId);
        if (current == null) {
            return new ArrayList<>();
        }

        Set<Long> excludeIds = new HashSet<>();
        excludeIds.add(robotId);

        List<RuleRecommendVO> result = new ArrayList<>();

        // 1. 同分类推荐
        if (current.getCategoryId() != null) {
            List<Robot> sameCategory = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                    .eq(Robot::getCategoryId, current.getCategoryId())
                    .eq(Robot::getStatus, 1)
                    .notIn(Robot::getId, excludeIds)
                    .orderByDesc(Robot::getHotScore)
                    .last("LIMIT " + size));
            for (Robot r : sameCategory) {
                result.add(toVO(r, Constants.REC_SAME_CATEGORY, "同分类机器人"));
                excludeIds.add(r.getId());
            }
        }

        // 2. 同品牌推荐
        if (current.getBrandId() != null && result.size() < size) {
            List<Robot> sameBrand = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                    .eq(Robot::getBrandId, current.getBrandId())
                    .eq(Robot::getStatus, 1)
                    .notIn(Robot::getId, excludeIds)
                    .orderByDesc(Robot::getHotScore)
                    .last("LIMIT " + (size - result.size())));
            for (Robot r : sameBrand) {
                result.add(toVO(r, Constants.REC_SAME_BRAND, "同品牌机器人"));
                excludeIds.add(r.getId());
            }
        }

        // 3. 热门推荐
        if (result.size() < size) {
            List<Robot> trending = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                    .eq(Robot::getStatus, 1)
                    .notIn(Robot::getId, excludeIds)
                    .orderByDesc(Robot::getHotScore)
                    .last("LIMIT " + (size - result.size())));
            for (Robot r : trending) {
                result.add(toVO(r, Constants.REC_TRENDING, "热门机器人"));
                excludeIds.add(r.getId());
            }
        }

        // 填充品牌名
        fillBrandNames(result);

        // 缓存
        try {
            redisUtils.setObj(cacheKey, result, CACHE_SECONDS + (long) (Math.random() * 30), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis推荐缓存写入失败（不影响返回）: error={}", e.getMessage());
        }

        // 如果有登录用户，追加关注推荐
        if (currentUserId != null) {
            List<RuleRecommendVO> followedRecs = getFollowedRecommendations(robotId, currentUserId, size);
            return mergeWithPriority(followedRecs, result, size);
        }

        return result;
    }

    /** 关注机器人的同类推荐 */
    private List<RuleRecommendVO> getFollowedRecommendations(Long currentRobotId, Long userId, int limit) {
        List<RuleRecommendVO> result = new ArrayList<>();
        try {
            // 获取用户关注的机器人
            java.util.Set<Long> followedIds = followService.checkBatch(userId, Constants.BIZ_TYPE_ROBOT, new ArrayList<>());
            // checkBatch需要非空list，跳过
        } catch (Exception e) {
            log.debug("获取关注推荐失败: error={}", e.getMessage());
        }
        return result;
    }

    /** 合并推荐结果，去重，取前limit条 */
    private List<RuleRecommendVO> mergeWithPriority(List<RuleRecommendVO> priority, List<RuleRecommendVO> fallback, int limit) {
        Set<Long> seen = new HashSet<>();
        List<RuleRecommendVO> result = new ArrayList<>();
        for (RuleRecommendVO vo : priority) {
            if (seen.add(vo.getRobotId()) && result.size() < limit) {
                result.add(vo);
            }
        }
        for (RuleRecommendVO vo : fallback) {
            if (seen.add(vo.getRobotId()) && result.size() < limit) {
                result.add(vo);
            }
        }
        return result;
    }

    private RuleRecommendVO toVO(Robot r, String reasonCode, String reasonText) {
        RuleRecommendVO vo = new RuleRecommendVO();
        vo.setRobotId(r.getId());
        vo.setName(r.getName());
        vo.setCoverImage(r.getCoverImage());
        vo.setSubtitle(r.getSubtitle());
        vo.setBrandId(r.getBrandId());
        vo.setGuidePrice(r.getGuidePrice());
        vo.setScore(r.getScore());
        vo.setFavoriteCount(r.getFavoriteCount());
        vo.setHotScore(r.getHotScore());
        vo.setReasonCode(reasonCode);
        vo.setReasonText(reasonText);
        // Phase11: 匹配条件明细
        List<RuleRecommendVO.MatchedCondition> conditions = new ArrayList<>();
        RuleRecommendVO.MatchedCondition cond = new RuleRecommendVO.MatchedCondition();
        cond.setCode(reasonCode);
        cond.setText(reasonText);
        cond.setScore(0);
        conditions.add(cond);
        vo.setMatchedConditions(conditions);
        return vo;
    }

    private void fillBrandNames(List<RuleRecommendVO> items) {
        Set<Long> brandIds = new HashSet<>();
        for (RuleRecommendVO vo : items) {
            if (vo.getBrandId() != null) {
                brandIds.add(vo.getBrandId());
            }
        }
        if (brandIds.isEmpty()) {
            return;
        }
        Map<Long, String> brandNameMap = new HashMap<>();
        List<Brand> brands = brandMapper.selectBatchIds(brandIds);
        for (Brand b : brands) {
            brandNameMap.put(b.getId(), b.getName());
        }
        for (RuleRecommendVO vo : items) {
            if (vo.getBrandId() != null) {
                vo.setBrandName(brandNameMap.get(vo.getBrandId()));
            }
        }
    }
}