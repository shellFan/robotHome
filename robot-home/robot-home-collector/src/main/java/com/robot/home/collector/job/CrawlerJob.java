package com.robot.home.collector.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robot.home.collector.entity.CrawlerSource;
import com.robot.home.collector.entity.CrawlerTask;
import com.robot.home.collector.engine.CrawlerEngine;
import com.robot.home.collector.mapper.CrawlerSourceMapper;
import com.robot.home.collector.mapper.CrawlerTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集定时任务调度器
 * 基于Spring @Scheduled实现定时触发采集
 * ⚠️ 测试环境通过 @Profile("!test") 禁用，避免定时任务干扰测试
 */
@Component
@Profile("!test")
public class CrawlerJob {

    private static final Logger log = LoggerFactory.getLogger(CrawlerJob.class);

    @Autowired
    private CrawlerSourceMapper sourceMapper;

    @Autowired
    private CrawlerTaskMapper taskMapper;

    @Autowired
    private CrawlerEngine crawlerEngine;

    /**
     * 定时检查并触发需要执行的采集任务
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000ms
    public void checkAndTriggerTasks() {
        log.info("Checking scheduled crawler tasks at {}", LocalDateTime.now());

        // 查询所有启用的数据源
        List<CrawlerSource> activeSources = sourceMapper.selectList(
                new LambdaQueryWrapper<CrawlerSource>()
                        .eq(CrawlerSource::getCrawlEnabled, 1)
                        .eq(CrawlerSource::getStatus, 1));

        for (CrawlerSource source : activeSources) {
            try {
                if (shouldCrawl(source)) {
                    triggerCrawl(source);
                }
            } catch (Exception e) {
                log.error("Failed to trigger crawl for source: {}", source.getSourceName(), e);
            }
        }
    }

    /**
     * 判断数据源是否应该执行采集
     */
    private boolean shouldCrawl(CrawlerSource source) {
        // 如果没有配置采集间隔，默认24小时
        int interval = source.getCrawlInterval() != null ? source.getCrawlInterval() : 86400;

        // 如果从未采集过，直接执行
        if (source.getLastCrawlTime() == null) {
            return true;
        }

        // 如果上次采集时间 + 间隔 < 当前时间，则执行
        LocalDateTime nextCrawlTime = source.getLastCrawlTime().plusSeconds(interval);
        return LocalDateTime.now().isAfter(nextCrawlTime);
    }

    /**
     * 触发指定数据源的采集任务
     */
    private void triggerCrawl(CrawlerSource source) {
        log.info("Triggering crawl for source: {}, id={}", source.getSourceName(), source.getId());

        // 检查是否有正在运行的任务
        Long runningCount = taskMapper.selectCount(
                new LambdaQueryWrapper<CrawlerTask>()
                        .eq(CrawlerTask::getSourceId, source.getId())
                        .eq(CrawlerTask::getStatus, "RUNNING"));

        if (runningCount > 0) {
            log.warn("Source {} has running task, skip triggering.", source.getSourceName());
            return;
        }

        // 创建新任务
        CrawlerTask task = new CrawlerTask();
        task.setSourceId(source.getId());
        task.setTaskType("FULL");
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 异步启动采集
        final Long taskId = task.getId();
        new Thread(() -> {
            try {
                // 重新查询确保数据最新
                CrawlerTask runningTask = taskMapper.selectById(taskId);
                if (runningTask == null) return;

                runningTask.setStatus("RUNNING");
                runningTask.setStartTime(LocalDateTime.now());
                runningTask.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(runningTask);

                crawlerEngine.startTask(source, runningTask);

                runningTask.setStatus("COMPLETED");
                runningTask.setEndTime(LocalDateTime.now());
                runningTask.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(runningTask);

                // 更新数据源上次采集时间
                source.setLastCrawlTime(LocalDateTime.now());
                source.setLastCrawlStatus("SUCCESS");
                sourceMapper.updateById(source);

                log.info("Crawl completed for source: {}", source.getSourceName());

            } catch (Exception e) {
                log.error("Crawl failed for source: {}", source.getSourceName(), e);

                CrawlerTask failedTask = taskMapper.selectById(taskId);
                if (failedTask != null) {
                    failedTask.setStatus("FAILED");
                    failedTask.setEndTime(LocalDateTime.now());
                    failedTask.setErrorMessage(e.getMessage());
                    failedTask.setUpdateTime(LocalDateTime.now());
                    taskMapper.updateById(failedTask);
                }

                source.setLastCrawlTime(LocalDateTime.now());
                source.setLastCrawlStatus("FAILED");
                sourceMapper.updateById(source);
            }
        }, "crawler-scheduled-" + source.getId()).start();
    }
}