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
 * 核心参数：核心线程数=4，最大线程数=16，队列容量=500，拒绝策略=CallerRunsPolicy
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
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("异步线程池已初始化: core=4, max=16, queue=500");
        return executor;
    }
}