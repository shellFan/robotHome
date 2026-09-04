package com.robot.home.ranking.service;

import com.robot.home.ranking.vo.RankingTypeVO;
import com.robot.home.robot.vo.RobotListVO;

import java.util.List;

/**
 * 排行榜服务
 */
public interface RankingService {

    /**
     * 排行榜列表（热度权重算法计算，结果缓存到 Redis）
     */
    List<RobotListVO> rank(String type, int limit, Long currentUserId);

    List<RankingTypeVO> types();

    /**
     * 重算并落库 robot.hot_score（定时任务调用）
     */
    int refreshHotScores();
}
