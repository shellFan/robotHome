package com.robot.home.ranking.service;

import com.robot.home.ranking.vo.RankingSnapshotVO;
import com.robot.home.ranking.vo.RankingTypeVO;
import com.robot.home.robot.vo.RobotListVO;

import java.util.List;

/**
 * 排行榜服务
 */
public interface RankingService {

    /**
     * 排行榜列表（热度权重算法计算，结果缓存到 Redis）
     *
     * @param type    榜单类型: hot/humanoid/quadruped/service/industrial/family/dev
     *                Phase9新增: follow/favorite/discussion/review/new_product/company_attention
     * @param limit   数量
     * @param currentUserId 当前用户ID
     * @param timeRange 时间范围: day/week/month/all（默认all）
     */
    List<RobotListVO> rank(String type, int limit, Long currentUserId, String timeRange);

    /**
     * @deprecated 使用 {@link #rank(String, int, Long, String)}
     */
    @Deprecated
    default List<RobotListVO> rank(String type, int limit, Long currentUserId) {
        return rank(type, limit, currentUserId, "all");
    }

    List<RankingTypeVO> types();

    /**
     * 重算并落库 robot.hot_score（定时任务调用）
     */
    int refreshHotScores();

    /**
     * Phase9: 将Redis ZSET排行榜持久化到ranking_snapshot表
     * 包含排名变化计算和上榜原因生成
     */
    int snapshotRankings();

    /**
     * Phase9: 获取带排名变化的榜单（从快照读取）
     */
    List<RankingSnapshotVO> rankWithChange(String type, int limit);

    /**
     * Phase9: 刷新权重缓存（管理接口可调用）
     */
    void refreshWeightCache();
}
