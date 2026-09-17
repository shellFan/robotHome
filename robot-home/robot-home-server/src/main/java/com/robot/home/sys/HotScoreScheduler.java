package com.robot.home.sys;

import com.robot.home.ranking.service.RankingService;
import com.robot.home.ranking.service.impl.RankingServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时任务：重算机器人热度分 + 排行榜快照持久化
 */
@Slf4j
@Component
@EnableScheduling
public class HotScoreScheduler {

    @Resource
    private RankingService rankingService;
    @Resource
    private RankingServiceImpl rankingServiceImpl;

    /**
     * 每 30 分钟重算一次热度分
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void refresh() {
        try {
            int count = rankingService.refreshHotScores();
            log.info("机器人热度分重算完成，共 {} 条", count);
        } catch (Exception e) {
            log.warn("机器人热度分重算失败：{}", e.getMessage());
        }
    }

    /**
     * 每小时将 Redis ZSET 排行榜持久化到 ranking_snapshot 表
     * 用于排行榜历史追踪和 Redis 故障恢复
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void snapshotRankings() {
        try {
            int count = rankingServiceImpl.snapshotRankings();
            log.info("排行榜快照持久化完成，共 {} 条", count);
        } catch (Exception e) {
            log.warn("排行榜快照持久化失败：{}", e.getMessage());
        }
    }
}
