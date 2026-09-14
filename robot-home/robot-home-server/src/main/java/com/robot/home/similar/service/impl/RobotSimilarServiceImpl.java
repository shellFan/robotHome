package com.robot.home.similar.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.similar.entity.RobotSimilarScore;
import com.robot.home.similar.mapper.RobotSimilarScoreMapper;
import com.robot.home.similar.service.RobotSimilarService;
import com.robot.home.similar.vo.SimilarRobotVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotParamValue;
import com.robot.home.robot.entity.RobotTag;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.mapper.RobotParamValueMapper;
import com.robot.home.robot.mapper.RobotTagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 相似机器人推荐服务实现
 * 评分算法：5维加权评分
 * - category(分类): 30%权重
 * - price(价格): 25%权重
 * - brand(品牌): 15%权重
 * - param(参数): 20%权重
 * - tag(标签): 10%权重
 */
@Service
public class RobotSimilarServiceImpl implements RobotSimilarService {

    private static final Logger log = LoggerFactory.getLogger(RobotSimilarServiceImpl.class);

    // 维度权重
    private static final BigDecimal W_CATEGORY = new BigDecimal("0.30");
    private static final BigDecimal W_PRICE = new BigDecimal("0.25");
    private static final BigDecimal W_BRAND = new BigDecimal("0.15");
    private static final BigDecimal W_PARAM = new BigDecimal("0.20");
    private static final BigDecimal W_TAG = new BigDecimal("0.10");

    // 价格相似度：价格差距在此比例内视为相似
    private static final double PRICE_SIMILAR_RATIO = 0.5;

    @Resource
    private RobotSimilarScoreMapper similarScoreMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotParamValueMapper paramValueMapper;
    @Resource
    private RobotTagMapper robotTagMapper;

    @Override
    public List<SimilarRobotVO> listSimilar(Long robotId, Integer limit) {
        if (limit == null || limit < 1) {
            limit = 10;
        }
        if (limit > 50) {
            limit = 50;
        }

        List<RobotSimilarScore> scores = similarScoreMapper.selectList(
                Wrappers.<RobotSimilarScore>lambdaQuery()
                        .eq(RobotSimilarScore::getRobotId, robotId)
                        .orderByDesc(RobotSimilarScore::getTotalScore)
                        .last("LIMIT " + limit));

        if (scores.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取相似机器人信息
        List<Long> similarIds = scores.stream()
                .map(RobotSimilarScore::getSimilarRobotId)
                .collect(Collectors.toList());
        Map<Long, Robot> robotMap = new HashMap<>();
        for (Long sid : similarIds) {
            Robot robot = robotMapper.selectById(sid);
            if (robot != null) {
                robotMap.put(sid, robot);
            }
        }

        return scores.stream()
                .filter(s -> robotMap.containsKey(s.getSimilarRobotId()))
                .map(s -> toVO(s, robotMap.get(s.getSimilarRobotId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculate(Long robotId) {
        Robot source = robotMapper.selectById(robotId);
        if (source == null) {
            throw new BusinessException("机器人不存在");
        }

        // 获取源机器人信息
        List<String> sourceTags = getRobotTags(robotId);
        Map<Long, String> sourceParams = getRobotParams(robotId);

        // 查询所有其他机器人
        List<Robot> allRobots = robotMapper.selectList(
                Wrappers.<Robot>lambdaQuery()
                        .ne(Robot::getId, robotId)
                        .eq(Robot::getDeleted, Constants.DELETED_NO));

        // 删除旧评分
        similarScoreMapper.delete(Wrappers.<RobotSimilarScore>lambdaQuery()
                .eq(RobotSimilarScore::getRobotId, robotId));

        // 计算并保存新评分
        for (Robot target : allRobots) {
            BigDecimal categoryScore = calcCategoryScore(source, target);
            BigDecimal priceScore = calcPriceScore(source, target);
            BigDecimal brandScore = calcBrandScore(source, target);
            BigDecimal paramScore = calcParamScore(sourceParams, target.getId());
            BigDecimal tagScore = calcTagScore(sourceTags, target.getId());

            BigDecimal totalScore = categoryScore.multiply(W_CATEGORY)
                    .add(priceScore.multiply(W_PRICE))
                    .add(brandScore.multiply(W_BRAND))
                    .add(paramScore.multiply(W_PARAM))
                    .add(tagScore.multiply(W_TAG))
                    .setScale(2, RoundingMode.HALF_UP);

            // 只保存有一定相似度的结果（totalScore > 10）
            if (totalScore.compareTo(new BigDecimal("10")) > 0) {
                RobotSimilarScore score = new RobotSimilarScore();
                score.setRobotId(robotId);
                score.setSimilarRobotId(target.getId());
                score.setCategoryScore(categoryScore);
                score.setPriceScore(priceScore);
                score.setBrandScore(brandScore);
                score.setParamScore(paramScore);
                score.setTagScore(tagScore);
                score.setTotalScore(totalScore);
                score.setReason(buildReason(categoryScore, priceScore, brandScore, paramScore, tagScore));
                similarScoreMapper.insert(score);
            }
        }

        log.info("相似度重算完成: robotId={}, similarCount={}", robotId, allRobots.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateAll() {
        List<Robot> robots = robotMapper.selectList(
                Wrappers.<Robot>lambdaQuery()
                        .eq(Robot::getDeleted, Constants.DELETED_NO));

        log.info("开始全量相似度重算, robotCount={}", robots.size());
        for (Robot robot : robots) {
            try {
                recalculate(robot.getId());
            } catch (Exception e) {
                log.error("重算失败: robotId={}", robot.getId(), e);
            }
        }
        log.info("全量相似度重算完成");
    }

    // ---- 评分算法 ----

    /**
     * 分类相似度：同分类=100，同父级=60，否则=0
     */
    private BigDecimal calcCategoryScore(Robot source, Robot target) {
        if (source.getCategoryId() == null || target.getCategoryId() == null) {
            return BigDecimal.ZERO;
        }
        if (source.getCategoryId().equals(target.getCategoryId())) {
            return new BigDecimal("100");
        }
        // 简化：不同分类=0（后续可扩展父级匹配）
        return BigDecimal.ZERO;
    }

    /**
     * 价格相似度：价格差距在50%以内线性衰减
     */
    private BigDecimal calcPriceScore(Robot source, Robot target) {
        if (source.getPrice() == null || target.getPrice() == null
                || source.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal diff = source.getPrice().subtract(target.getPrice()).abs();
        BigDecimal ratio = diff.divide(source.getPrice(), 4, RoundingMode.HALF_UP);
        if (ratio.doubleValue() <= PRICE_SIMILAR_RATIO) {
            // 线性衰减：ratio=0 -> 100, ratio=0.5 -> 0
            BigDecimal score = new BigDecimal("100").multiply(
                    new BigDecimal("1").subtract(ratio.divide(new BigDecimal("0.5"), 4, RoundingMode.HALF_UP)));
            return score.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 品牌相似度：同品牌=100，否则=0
     */
    private BigDecimal calcBrandScore(Robot source, Robot target) {
        if (source.getBrandId() == null || target.getBrandId() == null) {
            return BigDecimal.ZERO;
        }
        return source.getBrandId().equals(target.getBrandId()) ? new BigDecimal("100") : BigDecimal.ZERO;
    }

    /**
     * 参数相似度：Jaccard系数（交集/并集）
     */
    private BigDecimal calcParamScore(Map<Long, String> sourceParams, Long targetRobotId) {
        if (sourceParams.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Long, String> targetParams = getRobotParams(targetRobotId);
        if (targetParams.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算相同defId且value相同的参数数
        int match = 0;
        int total = 0;
        Set<Long> allDefIds = new HashSet<>();
        allDefIds.addAll(sourceParams.keySet());
        allDefIds.addAll(targetParams.keySet());

        for (Long defId : allDefIds) {
            total++;
            String sv = sourceParams.get(defId);
            String tv = targetParams.get(defId);
            if (sv != null && tv != null && sv.equals(tv)) {
                match++;
            }
        }

        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(match * 100).divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
    }

    /**
     * 标签相似度：Jaccard系数
     */
    private BigDecimal calcTagScore(List<String> sourceTags, Long targetRobotId) {
        if (sourceTags.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<String> targetTags = getRobotTags(targetRobotId);
        if (targetTags.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<String> intersection = new HashSet<>(sourceTags);
        intersection.retainAll(new HashSet<>(targetTags));

        Set<String> union = new HashSet<>(sourceTags);
        union.addAll(targetTags);

        if (union.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(intersection.size() * 100).divide(new BigDecimal(union.size()), 2, RoundingMode.HALF_UP);
    }

    // ---- 辅助方法 ----

    private List<String> getRobotTags(Long robotId) {
        List<RobotTag> tags = robotTagMapper.selectList(
                Wrappers.<RobotTag>lambdaQuery().eq(RobotTag::getRobotId, robotId));
        return tags.stream().map(RobotTag::getTagValue).collect(Collectors.toList());
    }

    private Map<Long, String> getRobotParams(Long robotId) {
        List<RobotParamValue> params = paramValueMapper.selectList(
                Wrappers.<RobotParamValue>lambdaQuery().eq(RobotParamValue::getRobotId, robotId));
        Map<Long, String> map = new HashMap<>();
        for (RobotParamValue pv : params) {
            map.put(pv.getDefId(), pv.getValue());
        }
        return map;
    }

    private String buildReason(BigDecimal categoryScore, BigDecimal priceScore,
                                BigDecimal brandScore, BigDecimal paramScore, BigDecimal tagScore) {
        List<String> reasons = new ArrayList<>();
        if (categoryScore.compareTo(new BigDecimal("50")) >= 0) {
            reasons.add("同类");
        }
        if (priceScore.compareTo(new BigDecimal("30")) >= 0) {
            reasons.add("价格相近");
        }
        if (brandScore.compareTo(new BigDecimal("50")) >= 0) {
            reasons.add("同品牌");
        }
        if (paramScore.compareTo(new BigDecimal("30")) >= 0) {
            reasons.add("参数相似");
        }
        if (tagScore.compareTo(new BigDecimal("30")) >= 0) {
            reasons.add("标签相似");
        }
        return reasons.isEmpty() ? "综合推荐" : String.join("、", reasons);
    }

    private SimilarRobotVO toVO(RobotSimilarScore score, Robot robot) {
        SimilarRobotVO vo = new SimilarRobotVO();
        vo.setRobotId(robot.getId());
        vo.setRobotName(robot.getName());
        vo.setImageUrl(robot.getImageUrl());
        vo.setPrice(robot.getPrice());
        vo.setTotalScore(score.getTotalScore());
        vo.setReason(score.getReason());

        // 分类名和品牌名（简化：用ID占位，前端可按需展示）
        vo.setCategoryName(robot.getCategoryId() != null ? String.valueOf(robot.getCategoryId()) : null);
        vo.setBrandName(robot.getBrandId() != null ? String.valueOf(robot.getBrandId()) : null);

        // 评分明细
        List<SimilarRobotVO.ScoreDetail> details = new ArrayList<>();
        details.add(buildDetail("分类", score.getCategoryScore()));
        details.add(buildDetail("价格", score.getPriceScore()));
        details.add(buildDetail("品牌", score.getBrandScore()));
        details.add(buildDetail("参数", score.getParamScore()));
        details.add(buildDetail("标签", score.getTagScore()));
        vo.setScoreDetails(details);

        return vo;
    }

    private SimilarRobotVO.ScoreDetail buildDetail(String dimension, BigDecimal score) {
        SimilarRobotVO.ScoreDetail d = new SimilarRobotVO.ScoreDetail();
        d.setDimension(dimension);
        d.setScore(score);
        return d;
    }
}