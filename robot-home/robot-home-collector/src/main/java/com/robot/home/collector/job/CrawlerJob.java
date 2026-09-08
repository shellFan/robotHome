package com.robot.home.collector.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.robot.home.collector.common.constants.CrawlerConstants;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 采集定时任务调度器
 * 基于Spring @Scheduled实现定时触发采集
 * 
 * Phase5.3重构：
 * - 任务创建时status=QUEUED（等待被抢占）
 * - 使用CAS原子更新抢占任务（QUEUED→RUNNING），防止多实例并发重复执行
 * - 支持多任务并发（配合CrawlTaskContext状态隔离）
 * 
 * ⚠️ 测试环境通过 @Profile("!test") 禁用，避免定时任务干扰测试
 */
@Component
@Profile("!test")
public class CrawlerJob {

    private static final Logger log = LoggerFactory.getLogger(CrawlerJob.class);

    /** 采集任务线程池，限制最大2个并发采集 */
    private final ExecutorService crawlExecutor = Executors.newFixedThreadPool(2, new CrawlerThreadFactory());

    @Autowired
    private CrawlerSourceMapper sourceMapper;

    @Autowired
    private CrawlerTaskMapper taskMapper;

    @Autowired
    private CrawlerEngine crawlerEngine;

    /** 采集线程工厂，命名线程便于排查 */
    private static class CrawlerThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "crawler-task-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }

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
     * 
     * Phase5.3: 使用CAS原子抢占机制
     * 1. 创建任务 status=QUEUED
     * 2. CAS原子更新: UPDATE SET status='RUNNING' WHERE id=? AND status='QUEUED'
     * 3. 只有affectedRows==1的线程才真正执行采集
     */
    private void triggerCrawl(CrawlerSource source) {
        log.info("Triggering crawl for source: {}, id={}", source.getSourceName(), source.getId());

        // 检查是否有正在运行或已入队的任务（QUEUED也算，避免重复创建）
        Long activeCount = taskMapper.selectCount(
                new LambdaQueryWrapper<CrawlerTask>()
                        .eq(CrawlerTask::getSourceId, source.getId())
                        .in(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_RUNNING, CrawlerConstants.TASK_STATUS_QUEUED));

        if (activeCount > 0) {
            log.warn("Source {} has active task (RUNNING or QUEUED), skip triggering.", source.getSourceName());
            return;
        }

        // 创建新任务，status=QUEUED（等待被调度器抢占）
        CrawlerTask task = new CrawlerTask();
        task.setSourceId(source.getId());
        task.setTaskType(CrawlerConstants.TASK_TYPE_FULL);
        task.setStatus(CrawlerConstants.TASK_STATUS_QUEUED);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);

        final Long taskId = task.getId();

        // 异步启动采集（使用线程池，避免原始Thread创建）
        crawlExecutor.submit(() -> {
            try {
                // CAS原子抢占：只有status=QUEUED才能更新为RUNNING
                // 防止多实例/多线程并发重复执行同一任务
                int affected = taskMapper.update(null,
                        new LambdaUpdateWrapper<CrawlerTask>()
                                .eq(CrawlerTask::getId, taskId)
                                .eq(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_QUEUED)
                                .set(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_RUNNING)
                                .set(CrawlerTask::getStartTime, LocalDateTime.now())
                                .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
                );

                if (affected == 0) {
                    log.warn("CAS claim failed for task {}, another instance may have claimed it. Skipping.", taskId);
                    return;
                }

                log.info("CAS claim succeeded for task {}, starting crawl...", taskId);

                // 抢占成功，重新查询最新task数据
                CrawlerTask runningTask = taskMapper.selectById(taskId);
                if (runningTask == null) {
                    log.error("Task {} not found after CAS claim, skipping.", taskId);
                    return;
                }

                crawlerEngine.startTask(source, runningTask);

                // 采集完成，更新状态
                taskMapper.update(null,
                        new LambdaUpdateWrapper<CrawlerTask>()
                                .eq(CrawlerTask::getId, taskId)
                                .set(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_COMPLETED)
                                .set(CrawlerTask::getEndTime, LocalDateTime.now())
                                .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
                );

                // 更新数据源上次采集时间
                source.setLastCrawlTime(LocalDateTime.now());
                source.setLastCrawlStatus("SUCCESS");
                sourceMapper.updateById(source);

                log.info("Crawl completed for source: {}", source.getSourceName());

            } catch (Exception e) {
                log.error("Crawl failed for source: {}", source.getSourceName(), e);

                // 更新任务状态为FAILED（使用CAS防止覆盖其他状态）
                taskMapper.update(null,
                        new LambdaUpdateWrapper<CrawlerTask>()
                                .eq(CrawlerTask::getId, taskId)
                                .in(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_RUNNING, CrawlerConstants.TASK_STATUS_QUEUED)
                                .set(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_FAILED)
                                .set(CrawlerTask::getErrorMessage,
                                        e.getMessage() != null && e.getMessage().length() > 500
                                                ? e.getMessage().substring(0, 500) : e.getMessage())
                                .set(CrawlerTask::getEndTime, LocalDateTime.now())
                                .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
                );

                source.setLastCrawlTime(LocalDateTime.now());
                source.setLastCrawlStatus("FAILED");
                sourceMapper.updateById(source);
            }
        });
    }
}