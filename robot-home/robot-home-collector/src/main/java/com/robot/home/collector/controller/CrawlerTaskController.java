package com.robot.home.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.collector.common.constants.CrawlerConstants;
import com.robot.home.collector.entity.CrawlerSource;
import com.robot.home.collector.entity.CrawlerTask;
import com.robot.home.collector.engine.CrawlerEngine;
import com.robot.home.collector.mapper.CrawlerSourceMapper;
import com.robot.home.collector.mapper.CrawlerTaskMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 采集任务管理API
 */
@RestController
@RequestMapping("/api/crawler/task")
public class CrawlerTaskController {

    private static final Logger log = LoggerFactory.getLogger(CrawlerTaskController.class);

    @Autowired
    private CrawlerTaskMapper taskMapper;

    @Autowired
    private CrawlerSourceMapper sourceMapper;

    @Autowired
    private CrawlerEngine crawlerEngine;

    /** 采集任务专用线程池（单线程，保证同一时间只运行一个采集任务） */
    private final ExecutorService crawlExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "crawler-task-executor");
        t.setDaemon(true);
        return t;
    });

    /**
     * 分页查询任务
     */
    @GetMapping("/list")
    public Page<CrawlerTask> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String status) {

        Page<CrawlerTask> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<CrawlerTask> wrapper = new LambdaQueryWrapper<>();

        if (sourceId != null) {
            wrapper.eq(CrawlerTask::getSourceId, sourceId);
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(CrawlerTask::getStatus, status);
        }
        wrapper.orderByDesc(CrawlerTask::getCreateTime);

        return taskMapper.selectPage(pageReq, wrapper);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public CrawlerTask get(@PathVariable Long id) {
        return taskMapper.selectById(id);
    }

    /**
     * 创建任务
     * Phase5.3: 任务创建时status=QUEUED，等待CAS抢占后变为RUNNING
     */
    @PostMapping
    public CrawlerTask create(@RequestBody CrawlerTask task) {
        task.setStatus(CrawlerConstants.TASK_STATUS_QUEUED);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);
        log.info("Created crawler task: id={}, sourceId={}, status=QUEUED", task.getId(), task.getSourceId());
        return task;
    }

    /**
     * 手动触发采集任务
     * Phase5.3: CAS原子抢占 — 只有QUEUED或FAILED状态的任务才能启动
     * 防止并发竞态：多个请求/实例同时尝试启动同一任务
     */
    @PostMapping("/{id}/start")
    public Map<String, Object> startTask(@PathVariable Long id) {
        // CAS原子更新：只有QUEUED或FAILED状态的任务才能启动，防止并发竞态
        int affected = taskMapper.update(null,
                new LambdaUpdateWrapper<CrawlerTask>()
                        .eq(CrawlerTask::getId, id)
                        .in(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_QUEUED, CrawlerConstants.TASK_STATUS_FAILED)
                        .set(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_RUNNING)
                        .set(CrawlerTask::getStartTime, LocalDateTime.now())
                        .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
        );

        if (affected == 0) {
            CrawlerTask existing = taskMapper.selectById(id);
            if (existing == null) {
                return errorResult("Task not found: " + id);
            }
            return errorResult("Task cannot be started: current status is " + existing.getStatus());
        }

        CrawlerTask task = taskMapper.selectById(id);
        CrawlerSource source = sourceMapper.selectById(task.getSourceId());
        if (source == null) {
            // 回滚状态
            taskMapper.update(null,
                    new LambdaUpdateWrapper<CrawlerTask>()
                            .eq(CrawlerTask::getId, id)
                            .set(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_FAILED)
                            .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
            );
            return errorResult("Source not found: " + task.getSourceId());
        }

        // 异步启动采集（使用线程池替代 new Thread）
        crawlExecutor.submit(() -> {
            try {
                crawlerEngine.startTask(source, task);
                // 只更新状态和结束时间，不覆盖计数器（引擎已通过updateTaskCounters更新）
                taskMapper.update(null,
                        new LambdaUpdateWrapper<CrawlerTask>()
                                .eq(CrawlerTask::getId, task.getId())
                                .set(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_COMPLETED)
                                .set(CrawlerTask::getEndTime, LocalDateTime.now())
                                .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
                );
            } catch (Exception e) {
                log.error("Crawl task failed", e);
                taskMapper.update(null,
                        new LambdaUpdateWrapper<CrawlerTask>()
                                .eq(CrawlerTask::getId, task.getId())
                                .set(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_FAILED)
                                .set(CrawlerTask::getErrorMessage, e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error")
                                .set(CrawlerTask::getEndTime, LocalDateTime.now())
                                .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
                );
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Task started: " + id);
        return result;
    }

    /**
     * 停止任务
     */
    @PostMapping("/{id}/stop")
    public Map<String, Object> stopTask(@PathVariable Long id) {
        CrawlerTask task = taskMapper.selectById(id);
        if (task == null) {
            return errorResult("Task not found: " + id);
        }

        if (!CrawlerConstants.TASK_STATUS_RUNNING.equals(task.getStatus())) {
            return errorResult("Task is not running: " + task.getStatus());
        }

        crawlerEngine.stopTask(id);
        taskMapper.update(null,
                new LambdaUpdateWrapper<CrawlerTask>()
                        .eq(CrawlerTask::getId, id)
                        .eq(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_RUNNING)
                        .set(CrawlerTask::getStatus, CrawlerConstants.TASK_STATUS_STOPPED)
                        .set(CrawlerTask::getEndTime, LocalDateTime.now())
                        .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Task stopped: " + id);
        return result;
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        taskMapper.deleteById(id);
        log.info("Deleted crawler task: id={}", id);
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}