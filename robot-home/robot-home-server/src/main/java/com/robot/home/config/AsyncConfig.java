package com.robot.home.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * <p>
 * 解决 Spring 默认 SimpleAsyncTaskExecutor 无限创建线程的问题。
 * 核心参数：核心线程数=4，最大线程数=16，队列容量=500
 * <p>
 * 拒绝策略：DiscardOldestPolicy + 日志记录
 * 行为数据可降级丢弃，CallerRunsPolicy会阻塞核心接口（如HTTP请求线程）不可接受
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        // 行为数据可降级丢弃，CallerRunsPolicy会阻塞调用线程（可能是HTTP请求线程）不可接受
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("异步任务被拒绝丢弃: activeCount={}, poolSize={}, queueSize={}",
                    e.getActiveCount(), e.getPoolSize(), e.getQueue().size());
            if (!e.isShutdown()) {
                // DiscardOldestPolicy语义：丢弃队列头部最老的任务，尝试提交新任务
                if (!e.getQueue().offer(r)) {
                    log.warn("丢弃队列头部后仍无法提交任务，彻底丢弃");
                }
            }
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("异步线程池已初始化: core=4, max=16, queue=500, rejectPolicy=DiscardOldestWithLog");
        return executor;
    }
}