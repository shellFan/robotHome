package com.robot.home.collector.engine;

import com.robot.home.collector.entity.CrawlerSource;
import com.robot.home.collector.entity.CrawlerTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 采集任务上下文 — 封装单个任务的所有可变状态
 * 每个采集任务创建独立的CrawlTaskContext，彻底隔离并发状态
 *
 * 解决的核心问题：CrawlerEngine是Spring Singleton Bean，
 * 原先的成员变量(urlQueue/visitedUrls/counters等)在多任务并发时互相污染
 */
public class CrawlTaskContext {

    // ===== 任务标识 =====
    private final CrawlerSource source;
    private final CrawlerTask task;

    // ===== URL队列（有界） =====
    private final PriorityBlockingQueue<CrawlerEngine.UrlTask> urlQueue;
    private final int maxQueueSize;

    // ===== 已访问URL集合 =====
    private final Set<String> visitedUrls;

    // ===== 运行控制 =====
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    // ===== 计数器 =====
    private final AtomicInteger urlsDiscovered = new AtomicInteger(0);
    private final AtomicInteger urlsSuccess = new AtomicInteger(0);
    private final AtomicInteger urlsFailed = new AtomicInteger(0);
    private final AtomicInteger articlesNew = new AtomicInteger(0);
    private final AtomicInteger articlesUpdated = new AtomicInteger(0);
    private final AtomicInteger articlesDuplicate = new AtomicInteger(0);
    private final AtomicInteger productsNew = new AtomicInteger(0);
    private final AtomicInteger productsUpdated = new AtomicInteger(0);
    private final AtomicInteger imagesDownloaded = new AtomicInteger(0);
    private final AtomicInteger imagesFailed = new AtomicInteger(0);

    // ===== 活跃工作线程计数 =====
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    // ===== 配置参数（从source覆盖全局默认值） =====
    private final int maxDepth;
    private final int maxUrlsPerTask;
    private final boolean respectRobots;

    /**
     * 构造CrawlTaskContext
     *
     * @param source        采集数据源
     * @param task          采集任务
     * @param maxDepth      最大爬取深度（source覆盖 > 全局默认）
     * @param maxUrlsPerTask 单任务最大URL数（source覆盖 > 全局默认）
     * @param maxQueueSize  URL队列硬上限
     * @param respectRobots 是否遵守robots.txt
     */
    public CrawlTaskContext(CrawlerSource source, CrawlerTask task,
                            int maxDepth, int maxUrlsPerTask,
                            int maxQueueSize, boolean respectRobots) {
        this.source = source;
        this.task = task;
        this.maxDepth = maxDepth;
        this.maxUrlsPerTask = maxUrlsPerTask;
        this.maxQueueSize = maxQueueSize;
        this.respectRobots = respectRobots;

        // 有界PriorityBlockingQueue：初始容量1000，通过offerWithLimit()强制硬上限
        this.urlQueue = new PriorityBlockingQueue<>(1000,
                java.util.Comparator.comparingInt(CrawlerEngine.UrlTask::getPriority).reversed());
        this.visitedUrls = ConcurrentHashMap.newKeySet();
    }

    // ===== 运行控制 =====

    /**
     * 请求停止采集（优雅停机）
     */
    public void requestStop() {
        stopFlag.set(true);
    }

    /**
     * 是否已请求停止
     */
    public boolean isStopRequested() {
        return stopFlag.get();
    }

    // ===== 队列操作（有界） =====

    /**
     * 入队URL（带硬上限检查）
     * @return true=入队成功, false=队列已满或已访问
     */
    public boolean enqueue(CrawlerEngine.UrlTask urlTask) {
        if (urlQueue.size() >= maxQueueSize) {
            return false; // 队列已满，拒绝入队
        }
        String urlHash = com.robot.home.collector.util.UrlNormalizer.hash(urlTask.getUrl());
        if (visitedUrls.contains(urlHash)) {
            return false; // 已访问
        }
        return urlQueue.offer(urlTask);
    }

    /**
     * 从队列中取出URL（阻塞5秒）
     */
    public CrawlerEngine.UrlTask pollUrl() throws InterruptedException {
        return urlQueue.poll(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 队列是否为空
     */
    public boolean isQueueEmpty() {
        return urlQueue.isEmpty();
    }

    /**
     * 获取队列当前大小
     */
    public int getQueueSize() {
        return urlQueue.size();
    }

    // ===== 已访问URL管理 =====

    /**
     * 标记URL为已访问（线程安全）
     * @return true=首次标记, false=已存在
     */
    public boolean markVisited(String urlHash) {
        return visitedUrls.add(urlHash);
    }

    /**
     * 检查URL是否已访问
     */
    public boolean isVisited(String urlHash) {
        return visitedUrls.contains(urlHash);
    }

    /**
     * 已访问URL数量
     */
    public int getVisitedCount() {
        return visitedUrls.size();
    }

    /**
     * 是否达到URL数量上限
     */
    public boolean isMaxUrlsReached() {
        return visitedUrls.size() >= maxUrlsPerTask;
    }

    // ===== 工作线程管理 =====

    public int incrementActiveWorkers() {
        return activeWorkers.incrementAndGet();
    }

    public int decrementActiveWorkers() {
        return activeWorkers.decrementAndGet();
    }

    public int getActiveWorkers() {
        return activeWorkers.get();
    }

    // ===== 计数器操作 =====

    public int incrementUrlsDiscovered() { return urlsDiscovered.incrementAndGet(); }
    public int addUrlsDiscovered(int delta) { return urlsDiscovered.addAndGet(delta); }
    public int incrementUrlsSuccess() { return urlsSuccess.incrementAndGet(); }
    public int incrementUrlsFailed() { return urlsFailed.incrementAndGet(); }
    public int incrementArticlesNew() { return articlesNew.incrementAndGet(); }
    public int incrementArticlesUpdated() { return articlesUpdated.incrementAndGet(); }
    public int incrementArticlesDuplicate() { return articlesDuplicate.incrementAndGet(); }
    public int incrementProductsNew() { return productsNew.incrementAndGet(); }
    public int incrementProductsUpdated() { return productsUpdated.incrementAndGet(); }
    public int incrementImagesDownloaded() { return imagesDownloaded.incrementAndGet(); }
    public int incrementImagesFailed() { return imagesFailed.incrementAndGet(); }

    // ===== 计数器读取 =====

    public int getUrlsDiscovered() { return urlsDiscovered.get(); }
    public int getUrlsSuccess() { return urlsSuccess.get(); }
    public int getUrlsFailed() { return urlsFailed.get(); }
    public int getArticlesNew() { return articlesNew.get(); }
    public int getArticlesUpdated() { return articlesUpdated.get(); }
    public int getArticlesDuplicate() { return articlesDuplicate.get(); }
    public int getProductsNew() { return productsNew.get(); }
    public int getProductsUpdated() { return productsUpdated.get(); }
    public int getImagesDownloaded() { return imagesDownloaded.get(); }
    public int getImagesFailed() { return imagesFailed.get(); }

    // ===== Getter =====

    public CrawlerSource getSource() { return source; }
    public CrawlerTask getTask() { return task; }
    public int getMaxDepth() { return maxDepth; }
    public int getMaxUrlsPerTask() { return maxUrlsPerTask; }
    public boolean isRespectRobots() { return respectRobots; }

    /**
     * 获取任务进度摘要（用于日志和数据库更新）
     */
    public String getProgressSummary() {
        return String.format("visited=%d, queue=%d, discovered=%d, success=%d, failed=%d, articles=%d, products=%d",
                visitedUrls.size(), urlQueue.size(), urlsDiscovered.get(),
                urlsSuccess.get(), urlsFailed.get(), articlesNew.get(), productsNew.get());
    }
}