package com.robot.home.recommend.service;

import com.robot.home.recommend.vo.RuleRecommendVO;

import java.util.List;

/**
 * Phase9: 规则推荐服务（可解释、无AI）
 */
public interface RuleRecommendService {

    /**
     * 获取机器人相关推荐
     * 规则: 同分类 > 同品牌 > 热门 > 关注机器人的同类
     *
     * @param robotId       当前机器人ID
     * @param currentUserId 当前用户ID（可选，用于关注推荐）
     * @param limit         数量
     */
    List<RuleRecommendVO> relatedRobots(Long robotId, Long currentUserId, int limit);
}