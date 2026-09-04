package com.robot.home.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 采集器配置
 */
@Configuration
@ConfigurationProperties(prefix = "crawler")
public class CrawlerConfig {

    /** 最大爬取深度 */
    private int maxDepth = 3;

    /** 每个任务最大URL数 */
    private int maxUrlsPerTask = 1000;

    /** 工作线程数 */
    private int threadCount = 3;

    /** 是否遵守robots.txt */
    private boolean respectRobots = true;

    /** 请求间隔（毫秒） */
    private int minInterval = 1000;

    /** 最大重试次数 */
    private int maxRetries = 3;

    /** 重试间隔基数（毫秒，指数退避） */
    private int retryBaseInterval = 1000;

    /** HTTP连接超时（毫秒） */
    private int connectTimeout = 10000;

    /** HTTP读取超时（毫秒） */
    private int readTimeout = 30000;

    /** User-Agent */
    private String userAgent = "RobotHomeCrawler/1.0";

    /** 图片存储类型：local, minio */
    private String imageStorageType = "local";

    /** 本地图片存储路径 */
    private String imageLocalPath = "/tmp/crawler/images";

    /** 浏览器模式：是否启用Playwright */
    private boolean browserEnabled = false;

    /** 浏览器等待时间（毫秒） */
    private int browserWaitAfterLoad = 2000;

    // Getters and Setters
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    public int getMaxUrlsPerTask() { return maxUrlsPerTask; }
    public void setMaxUrlsPerTask(int maxUrlsPerTask) { this.maxUrlsPerTask = maxUrlsPerTask; }
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    public boolean isRespectRobots() { return respectRobots; }
    public void setRespectRobots(boolean respectRobots) { this.respectRobots = respectRobots; }
    public int getMinInterval() { return minInterval; }
    public void setMinInterval(int minInterval) { this.minInterval = minInterval; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public int getRetryBaseInterval() { return retryBaseInterval; }
    public void setRetryBaseInterval(int retryBaseInterval) { this.retryBaseInterval = retryBaseInterval; }
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getImageStorageType() { return imageStorageType; }
    public void setImageStorageType(String imageStorageType) { this.imageStorageType = imageStorageType; }
    public String getImageLocalPath() { return imageLocalPath; }
    public void setImageLocalPath(String imageLocalPath) { this.imageLocalPath = imageLocalPath; }
    public boolean isBrowserEnabled() { return browserEnabled; }
    public void setBrowserEnabled(boolean browserEnabled) { this.browserEnabled = browserEnabled; }
    public int getBrowserWaitAfterLoad() { return browserWaitAfterLoad; }
    public void setBrowserWaitAfterLoad(int browserWaitAfterLoad) { this.browserWaitAfterLoad = browserWaitAfterLoad; }
}