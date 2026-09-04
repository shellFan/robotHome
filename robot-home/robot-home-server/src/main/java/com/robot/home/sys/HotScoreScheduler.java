package com.robot.home.sys;

import com.robot.home.ranking.service.RankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时任务：重算机器人热度分并刷新榜单缓存
 */
@Slf4j
@Component
@EnableScheduling
public class HotScoreScheduler {

    @Resource
    private RankingService rankingService;

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
}
