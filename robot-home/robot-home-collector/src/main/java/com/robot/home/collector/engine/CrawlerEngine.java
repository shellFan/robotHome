package com.robot.home.collector.engine;

import com.robot.home.collector.adapter.CrawlerAdapter;
import com.robot.home.collector.adapter.GenericWebsiteAdapter;
import com.robot.home.collector.adapter.ParsedData;
import com.robot.home.collector.adapter.WeChatAdapter;
import com.robot.home.collector.entity.*;
import com.robot.home.collector.fetcher.BrowserFetcher;
import com.robot.home.collector.fetcher.FetchResult;
import com.robot.home.collector.fetcher.HttpFetcher;
import com.robot.home.collector.mapper.CrawlerErrorMapper;
import com.robot.home.collector.mapper.CrawlerTaskMapper;
import com.robot.home.collector.parser.DateParser;
import com.robot.home.collector.parser.PageClassifier;
import com.robot.home.collector.parser.RobotsTxtParser;
import com.robot.home.collector.parser.RssAtomParser;
import com.robot.home.collector.parser.SitemapParser;
import com.robot.home.collector.service.*;
import com.robot.home.collector.util.HashUtils;
import com.robot.home.collector.util.HtmlSanitizer;
import com.robot.home.collector.util.TextCleanUtils;
import com.robot.home.collector.util.UrlNormalizer;
import com.robot.home.collector.util.UrlSecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 采集引擎核心
 * 负责任务调度、URL队列管理、适配器路由、状态机流转
 *
 * Phase5.3重构：所有任务级可变状态已迁移至CrawlTaskContext，
 * CrawlerEngine作为无状态执行服务，支持多任务并发安全
 */
@Component
public class CrawlerEngine {

    private static final Logger log = LoggerFactory.getLogger(CrawlerEngine.class);

    /** 默认URL队列硬上限 */
    private static final int DEFAULT_MAX_QUEUE_SIZE = 10000;

    /** 心跳更新间隔（秒） */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    @Autowired
    private HttpFetcher httpFetcher;

    @Autowired
    private BrowserFetcher browserFetcher;

    @Autowired
    private DeduplicationService deduplicationService;

    @Autowired
    private BrandMatchService brandMatchService;

    @Autowired
    private ParamNormalizeService paramNormalizeService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private GenericWebsiteAdapter genericWebsiteAdapter;

    @Autowired
    private WeChatAdapter weChatAdapter;

    @Autowired
    private RobotsTxtParser robotsTxtParser;

    @Autowired
    private SitemapParser sitemapParser;

    @Autowired
    private RssAtomParser rssAtomParser;

    @Autowired
    private DateParser dateParser;

    @Autowired
    private PageClassifier pageClassifier;

    @Autowired
    private SourceHealthMonitor sourceHealthMonitor;

    @Autowired
    private CrawlerTaskMapper taskMapper;

    @Autowired
    private CrawlerErrorMapper errorMapper;

    @Value("${crawler.max-depth:3}")
    private int defaultMaxDepth;

    @Value("${crawler.max-urls-per-task:1000}")
    private int defaultMaxUrlsPerTask;

    @Value("${crawler.thread-count:3}")
    private int threadCount;

    @Value("${crawler.respect-robots:true}")
    private boolean defaultRespectRobots;

    /** 适配器注册表（全局共享，线程安全） */
    private final Map<String, CrawlerAdapter> adapterRegistry = new ConcurrentHashMap<>();

    /** 活跃任务上下文表（taskId → CrawlTaskContext），支持多任务并发 */
    private final ConcurrentHashMap<Long, CrawlTaskContext> activeContexts = new ConcurrentHashMap<>();

    /** 活跃任务线程池表（taskId → ExecutorService），每个任务独立线程池 */
    private final ConcurrentHashMap<Long, ExecutorService> taskExecutors = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册默认适配器
        registerAdapter(genericWebsiteAdapter);
        // 注册微信适配器
        registerAdapter(weChatAdapter);

        // 崩溃恢复：将上次异常退出时遗留的RUNNING/QUEUED任务重置为FAILED，允许重新执行
        try {
            int recovered = taskMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrawlerTask>()
                            .in(CrawlerTask::getStatus, "RUNNING", "QUEUED")
                            .set(CrawlerTask::getStatus, "FAILED")
                            .set(CrawlerTask::getErrorMessage, "Task interrupted by server restart")
                            .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
            );
            if (recovered > 0) {
                log.info("Crash recovery: reset {} RUNNING/QUEUED task(s) to FAILED", recovered);
            }
        } catch (Exception e) {
            log.warn("Crash recovery check failed (non-critical): {}", e.getMessage());
        }
    }

    /**
     * 优雅停机：Spring容器关闭时停止所有正在运行的采集任务
     */
    @PreDestroy
    public void destroy() {
        // 停止所有活跃任务
        for (Map.Entry<Long, CrawlTaskContext> entry : activeContexts.entrySet()) {
            CrawlTaskContext ctx = entry.getValue();
            if (!ctx.isStopRequested()) {
                log.info("Shutting down task {} gracefully...", entry.getKey());
                ctx.requestStop();
                CrawlerTask task = ctx.getTask();
                if (task != null) {
                    try {
                        taskMapper.update(null,
                                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrawlerTask>()
                                        .eq(CrawlerTask::getId, task.getId())
                                        .set(CrawlerTask::getStatus, "FAILED")
                                        .set(CrawlerTask::getErrorMessage, "Task interrupted by server shutdown")
                                        .set(CrawlerTask::getEndTime, LocalDateTime.now())
                                        .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
                        );
                    } catch (Exception e) {
                        log.warn("Failed to update task status during shutdown: {}", e.getMessage());
                    }
                }
            }
        }
        // 关闭所有线程池
        for (Map.Entry<Long, ExecutorService> entry : taskExecutors.entrySet()) {
            shutdownExecutor(entry.getValue());
        }
        activeContexts.clear();
        taskExecutors.clear();
    }

    /**
     * 注册适配器
     */
    public void registerAdapter(CrawlerAdapter adapter) {
        adapterRegistry.put(adapter.getType(), adapter);
        log.info("Registered crawler adapter: {}", adapter.getType());
    }

    /**
     * 启动采集任务
     * 所有任务级状态封装在CrawlTaskContext中，引擎本身无状态
     * 支持多任务并发：每个任务通过taskId独立追踪上下文和线程池
     */
    public void startTask(CrawlerSource source, CrawlerTask task) {
        Long taskId = task.getId();

        // 检查是否已有同名任务在运行
        if (activeContexts.containsKey(taskId)) {
            log.warn("Task {} is already running, skip.", taskId);
            return;
        }

        // 解析source级配置覆盖全局默认值
        int maxDepth = source.getMaxDepth() != null ? source.getMaxDepth() : defaultMaxDepth;
        int maxUrlsPerTask = source.getMaxPages() != null ? source.getMaxPages() : defaultMaxUrlsPerTask;
        boolean respectRobots = source.getRespectRobots() != null ? source.getRespectRobots() == 1 : defaultRespectRobots;

        // 创建独立的任务上下文
        CrawlTaskContext ctx = new CrawlTaskContext(source, task, maxDepth, maxUrlsPerTask,
                DEFAULT_MAX_QUEUE_SIZE, respectRobots);
        activeContexts.put(taskId, ctx);

        log.info("Starting crawl task: source={}, task={}, baseUrl={}, maxDepth={}, maxUrls={}, respectRobots={}",
                source.getSourceName(), taskId, source.getBaseUrl(), maxDepth, maxUrlsPerTask, respectRobots);

        // 加载采集源健康状态
        sourceHealthMonitor.loadFromEntity(source);

        // 初始化种子URL
        initializeSeedUrls(ctx);
        log.info("Seed URLs initialized: {} URLs in queue", ctx.getQueueSize());

        if (ctx.isQueueEmpty()) {
            log.warn("No seed URLs found for source: {}. Task will complete with 0 results.", source.getSourceName());
            ctx.requestStop();
            updateTaskCounters(ctx);
            activeContexts.remove(taskId);
            return;
        }

        // 创建该任务的独立线程池
        ExecutorService taskExecutor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "crawler-worker-task" + taskId + "-" + System.currentTimeMillis() % 1000);
            t.setDaemon(true);
            return t;
        });
        taskExecutors.put(taskId, taskExecutor);

        // 启动工作线程
        for (int i = 0; i < threadCount; i++) {
            taskExecutor.submit(() -> workerLoop(ctx));
        }

        // 等待队列清空或任务停止（同时等待活跃工作线程完成，防止过早退出）
        int lastUpdateSize = 0;
        long lastHeartbeatTime = System.currentTimeMillis();
        while (!ctx.isStopRequested() && (!ctx.isQueueEmpty() || ctx.getActiveWorkers() > 0)) {
            try {
                Thread.sleep(2000);

                // 定期更新任务计数器到数据库
                int currentSize = ctx.getVisitedCount();
                if (currentSize != lastUpdateSize) {
                    updateTaskCounters(ctx);
                    lastUpdateSize = currentSize;
                    log.info("Crawl progress: {}", ctx.getProgressSummary());
                }

                // 心跳更新：防止任务被误判为超时
                long now = System.currentTimeMillis();
                if (now - lastHeartbeatTime >= HEARTBEAT_INTERVAL_SECONDS * 1000L) {
                    heartbeatUpdate(task);
                    lastHeartbeatTime = now;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 等待工作线程完成当前URL
        shutdownExecutor(taskExecutor);
        taskExecutors.remove(taskId);

        // 最终更新计数器
        updateTaskCounters(ctx);

        // 同步健康度到数据库
        try {
            sourceHealthMonitor.syncToEntity(source);
            log.info("Synced health status for source: {}", source.getSourceName());
        } catch (Exception e) {
            log.warn("Failed to sync health status: {}", e.getMessage());
        }

        log.info("Crawl task completed: source={}, {}", source.getSourceName(), ctx.getProgressSummary());
        activeContexts.remove(taskId);
    }

    /**
     * 停止指定任务（通过stopFlag实现，工作线程在当前URL完成后退出）
     */
    public void stopTask(Long taskId) {
        CrawlTaskContext ctx = activeContexts.get(taskId);
        if (ctx != null) {
            ctx.requestStop();
            log.info("Stop requested for task: {}", taskId);
        } else {
            log.warn("Task {} not found in active contexts", taskId);
        }
    }

    /**
     * 停止所有活跃任务
     */
    public void stopAll() {
        log.info("Requesting stop for all {} active tasks", activeContexts.size());
        activeContexts.values().forEach(CrawlTaskContext::requestStop);
    }

    /**
     * @deprecated 使用 stopTask(Long taskId) 替代
     */
    @Deprecated
    public void stop() {
        stopAll();
    }

    /**
     * 获取指定任务的活跃上下文
     */
    public CrawlTaskContext getActiveContext(Long taskId) {
        return activeContexts.get(taskId);
    }

    /**
     * 获取所有活跃任务上下文
     */
    public Map<Long, CrawlTaskContext> getAllActiveContexts() {
        return Collections.unmodifiableMap(activeContexts);
    }

    /**
     * @deprecated 使用 getActiveContext(Long taskId) 替代
     */
    @Deprecated
    public CrawlTaskContext getActiveContext() {
        // 向后兼容：返回任意一个活跃上下文
        return activeContexts.values().stream().findFirst().orElse(null);
    }

    /**
     * 优雅关闭线程池
     */
    private void shutdownExecutor(ExecutorService es) {
        if (es != null) {
            es.shutdownNow();
            try {
                es.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 心跳更新：更新任务updateTime防止被误判为超时
     */
    private void heartbeatUpdate(CrawlerTask task) {
        try {
            CrawlerTask updateTask = new CrawlerTask();
            updateTask.setId(task.getId());
            updateTask.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(updateTask);
        } catch (Exception e) {
            log.warn("Heartbeat update failed for task {}: {}", task.getId(), e.getMessage());
        }
    }

    /**
     * 更新任务计数器到数据库
     */
    private void updateTaskCounters(CrawlTaskContext ctx) {
        try {
            CrawlerTask updateTask = new CrawlerTask();
            updateTask.setId(ctx.getTask().getId());
            updateTask.setUrlsDiscovered(ctx.getUrlsDiscovered());
            updateTask.setUrlsSuccess(ctx.getUrlsSuccess());
            updateTask.setUrlsFailed(ctx.getUrlsFailed());
            updateTask.setArticlesNew(ctx.getArticlesNew());
            updateTask.setArticlesUpdated(ctx.getArticlesUpdated());
            updateTask.setArticlesDuplicate(ctx.getArticlesDuplicate());
            updateTask.setProductsNew(ctx.getProductsNew());
            updateTask.setProductsUpdated(ctx.getProductsUpdated());
            updateTask.setImagesDownloaded(ctx.getImagesDownloaded());
            updateTask.setImagesFailed(ctx.getImagesFailed());
            updateTask.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(updateTask);
        } catch (Exception e) {
            log.warn("Failed to update task counters: {}", e.getMessage());
        }
    }

    /**
     * 初始化种子URL
     */
    private void initializeSeedUrls(CrawlTaskContext ctx) {
        CrawlerSource source = ctx.getSource();
        CrawlerTask task = ctx.getTask();
        List<String> seedUrls = new ArrayList<>();

        // 从数据源配置获取种子URL
        if (StringUtils.isNotBlank(source.getSeedUrls())) {
            seedUrls.addAll(Arrays.asList(source.getSeedUrls().split("\\n")));
            log.info("Loaded {} seed URLs from source config", seedUrls.size());
        }

        // 从sitemap获取URL（支持gzip和sitemap index）
        if (StringUtils.isNotBlank(source.getSitemapUrl())) {
            try {
                FetchResult result = httpFetcher.fetch(source.getSitemapUrl());
                if (result.isSuccess()) {
                    SitemapParser.SitemapResult sitemapResult = sitemapParser.parse(result.getHtml());
                    
                    // 处理sitemap index（递归获取子sitemap）
                    if (sitemapResult.hasSitemapIndexes()) {
                        log.info("Found sitemap index with {} sub-sitemaps", sitemapResult.getSitemapIndexes().size());
                        int subIndex = 0;
                        for (String subSitemapUrl : sitemapResult.getSitemapIndexes()) {
                            if (subIndex >= 5) { // 最多递归5个子sitemap
                                log.warn("Reached max sub-sitemap limit (5), skipping remaining");
                                break;
                            }
                            try {
                                FetchResult subResult = httpFetcher.fetch(subSitemapUrl);
                                if (subResult.isSuccess()) {
                                    // 尝试gzip解析
                                    SitemapParser.SitemapResult subResult2;
                                    if (subSitemapUrl.endsWith(".gz") || subResult.getContentType().contains("gzip")) {
                                        subResult2 = sitemapParser.parseGzip(subResult.getBody());
                                    } else {
                                        subResult2 = sitemapParser.parse(subResult.getHtml());
                                    }
                                    sitemapResult.getUrls().addAll(subResult2.getUrls());
                                    subIndex++;
                                    log.info("Parsed sub-sitemap: {} URLs from {}", subResult2.getUrls().size(), subSitemapUrl);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to fetch sub-sitemap: {} - {}", subSitemapUrl, e.getMessage());
                            }
                        }
                    }

                    List<SitemapParser.SitemapUrl> sitemapUrls = sitemapResult.getUrls();
                    // 按优先级排序
                    sitemapUrls = sitemapParser.sortByPriority(sitemapUrls);
                    for (SitemapParser.SitemapUrl su : sitemapUrls) {
                        seedUrls.add(su.getLoc());
                    }
                    log.info("Discovered {} URLs from sitemap", sitemapUrls.size());
                } else {
                    log.warn("Failed to fetch sitemap: {} -> HTTP {}", source.getSitemapUrl(), result.getStatusCode());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch sitemap: {}", source.getSitemapUrl(), e.getMessage());
            }
        }

        // 从RSS/Atom feed获取URL
        if (StringUtils.isNotBlank(source.getRssUrl())) {
            try {
                FetchResult rssResult = httpFetcher.fetch(source.getRssUrl());
                if (rssResult.isSuccess() && rssResult.isXml()) {
                    RssAtomParser.FeedResult feedResult = rssAtomParser.parse(rssResult.getHtml());
                    List<String> feedUrls = rssAtomParser.extractUrls(feedResult);
                    seedUrls.addAll(feedUrls);
                    log.info("Discovered {} URLs from RSS/Atom feed: {}", feedUrls.size(), source.getRssUrl());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch RSS/Atom feed: {} - {}", source.getRssUrl(), e.getMessage());
            }
        }

        // 如果没有配置种子URL和sitemap，使用baseUrl作为起始URL
        if (seedUrls.isEmpty() && StringUtils.isNotBlank(source.getBaseUrl())) {
            seedUrls.add(source.getBaseUrl());
            log.info("Using baseUrl as seed URL: {}", source.getBaseUrl());
        }

        // 规范化并加入队列
        int addedCount = 0;
        for (String url : seedUrls) {
            String normalized = UrlNormalizer.normalize(url.trim());
            if (StringUtils.isNotBlank(normalized)) {
                UrlTask urlTask = new UrlTask(normalized, 0, 10, task.getId()); // 种子URL高优先级
                if (ctx.enqueue(urlTask)) {
                    addedCount++;
                }
            }
        }

        ctx.addUrlsDiscovered(addedCount);
        log.info("Initialized {} seed URLs for source: {} (total {} raw seeds)", addedCount, source.getSourceName(), seedUrls.size());
    }

    /**
     * 工作线程循环
     */
    private void workerLoop(CrawlTaskContext ctx) {
        while (!ctx.isStopRequested()) {
            try {
                UrlTask urlTask = ctx.pollUrl();
                if (urlTask == null) {
                    if (ctx.isQueueEmpty() && ctx.getActiveWorkers() == 0) break;
                    continue;
                }

                ctx.incrementActiveWorkers();
                try {
                    processUrl(urlTask, ctx);
                } finally {
                    ctx.decrementActiveWorkers();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker error", e);
            }
        }
    }

    /**
     * 处理单个URL
     */
    private void processUrl(UrlTask urlTask, CrawlTaskContext ctx) {
        String url = urlTask.getUrl();
        String urlHash = UrlNormalizer.hash(url);
        CrawlerSource source = ctx.getSource();
        CrawlerTask task = ctx.getTask();

        // 检查采集源健康度（DISABLED状态跳过）
        if (!sourceHealthMonitor.canCrawl(source.getId())) {
            log.warn("Source {} is DISABLED, skipping URL: {}", source.getId(), url);
            return;
        }

        // 检查是否已访问
        if (!ctx.markVisited(urlHash)) {
            log.info("URL already visited in this task: {}", url);
            return;
        }

        // 检查robots.txt
        if (ctx.isRespectRobots() && !robotsTxtParser.isAllowed(url, "RobotHomeCrawler")) {
            log.warn("Blocked by robots.txt: {}", url);
            ctx.incrementUrlsFailed();
            return;
        }

        // 检查URL去重（跨任务去重）
        if (deduplicationService.isUrlDuplicate(url)) {
            log.info("URL already processed in previous task: {}", url);
            ctx.incrementArticlesDuplicate();
            return;
        }

        // 检查URL数量限制
        if (ctx.isMaxUrlsReached()) {
            log.info("Reached max URLs limit: {}", ctx.getMaxUrlsPerTask());
            ctx.requestStop();
            return;
        }

        log.info("Processing URL: {} (depth={}, visited={}/{})", url, urlTask.getDepth(), ctx.getVisitedCount(), ctx.getMaxUrlsPerTask());

        try {
            // 1. 抓取页面
            FetchResult fetchResult = fetchPage(url, source);
            
            // 处理重定向：将重定向URL加入队列
            if (fetchResult.getStatusCode() >= 300 && fetchResult.getStatusCode() < 400) {
                String redirectUrl = fetchResult.getRedirectedUrl();
                if (StringUtils.isNotBlank(redirectUrl)) {
                    // 解析相对路径
                    if (!redirectUrl.startsWith("http")) {
                        redirectUrl = UrlNormalizer.resolve(url, redirectUrl);
                    }
                    // SSRF防护：校验重定向目标URL
                    if (!UrlSecurityUtil.isAllowedUrl(redirectUrl)) {
                        log.warn("SSRF protection: redirect to private/blocked URL blocked: {} -> {}", url, redirectUrl);
                        ctx.incrementUrlsFailed();
                        deduplicationService.markUrlFetched(url, "FAILED");
                        return;
                    }
                    log.info("Redirect detected: {} -> {} (HTTP {})", url, redirectUrl, fetchResult.getStatusCode());
                    if (urlTask.getDepth() < ctx.getMaxDepth() && shouldFollowDomain(redirectUrl, source)) {
                        UrlTask redirectTask = new UrlTask(redirectUrl, urlTask.getDepth(), urlTask.getPriority(), task.getId());
                        if (ctx.enqueue(redirectTask)) {
                            ctx.incrementUrlsDiscovered();
                        }
                    }
                } else {
                    log.warn("Redirect without Location header: {} -> HTTP {}", url, fetchResult.getStatusCode());
                }
                ctx.incrementUrlsFailed();
                // 重定向失败，标记为FAILED允许重试
                deduplicationService.markUrlFetched(url, "FAILED");
                return;
            }

            // 检查抓取是否成功
            if (!fetchResult.isSuccess()) {
                log.warn("Fetch failed for {}: HTTP {}, html={}, body={}, error={}", url, fetchResult.getStatusCode(),
                        fetchResult.getHtml() != null ? fetchResult.getHtml().length() : "null",
                        fetchResult.getBody() != null ? fetchResult.getBody().length : "null",
                        fetchResult.getError());
                ctx.incrementUrlsFailed();
                recordError(url, task.getId(), source.getId(), "FETCH_ERROR", "HTTP " + fetchResult.getStatusCode());
                // 记录健康度失败
                sourceHealthMonitor.recordFailure(source.getId(), "HTTP " + fetchResult.getStatusCode(), fetchResult.getFetchTimeMs());
                // 标记为FAILED，允许后续重试
                deduplicationService.markUrlFetched(url, "FAILED");
                return;
            }

            log.info("Fetch success: {} -> HTTP {}, size={} bytes, type={}",
                    url, fetchResult.getStatusCode(),
                    fetchResult.getHtml() != null ? fetchResult.getHtml().length() : (fetchResult.getBody() != null ? fetchResult.getBody().length : 0),
                    fetchResult.getContentType());

            // 2. 构建数据源配置
            Map<String, String> config = buildConfig(source);

            // 3. 选择适配器
            CrawlerAdapter adapter = selectAdapter(url, source, config);
            log.debug("Selected adapter: {} for url={}", adapter.getType(), url);

            // 4. 解析页面
            ParsedData parsedData = adapter.parse(fetchResult, config);
            if (parsedData == null || !parsedData.isSuccess()) {
                log.warn("Parse failed for url={}, reason={}", url,
                        parsedData != null ? parsedData.getError() : "null result");
                ctx.incrementUrlsFailed();
                // 解析失败，标记为FAILED允许重试
                deduplicationService.markUrlFetched(url, "FAILED");
                return;
            }

            // 5. 处理解析结果
            processParsedData(parsedData, url, source, task, urlTask.getDepth(), ctx);
            ctx.incrementUrlsSuccess();

            // 记录健康度成功
            sourceHealthMonitor.recordSuccess(source.getId(), fetchResult.getFetchTimeMs());

            // 6. 提取链接并加入队列
            if (urlTask.getDepth() < ctx.getMaxDepth()) {
                List<String> links = adapter.extractLinks(fetchResult, config);
                int newLinksCount = 0;
                for (String link : links) {
                    String normalized = UrlNormalizer.normalize(link);
                    if (adapter.shouldFollow(normalized, urlTask.getDepth() + 1, ctx.getMaxDepth(), config)) {
                        if (shouldFollowDomain(normalized, source)) {
                            UrlTask linkTask = new UrlTask(normalized, urlTask.getDepth() + 1, 1, task.getId());
                            if (ctx.enqueue(linkTask)) {
                                newLinksCount++;
                            }
                        }
                    }
                }
                if (newLinksCount > 0) {
                    ctx.addUrlsDiscovered(newLinksCount);
                    log.info("Discovered {} new links from: {}", newLinksCount, url);
                }
            }

            // 7. 标记URL和内容已抓取
            String contentText = TextCleanUtils.htmlToText(
                    parsedData.getContentHtml() != null ? parsedData.getContentHtml() : parsedData.getContent());
            deduplicationService.markFetched(url, contentText);
            deduplicationService.markUrlFetched(url, "SUCCESS");

        } catch (Exception e) {
            log.error("Error processing URL: {}", url, e);
            ctx.incrementUrlsFailed();
            String errorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
            recordError(url, task.getId(), source.getId(), "PROCESS_ERROR", errorMsg);
            // 记录健康度失败
            sourceHealthMonitor.recordFailure(source.getId(), errorMsg, 0);
            // 处理异常时标记URL为FAILED，允许后续重试
            deduplicationService.markUrlFetched(url, "FAILED");
        }
    }

    /**
     * 抓取页面（根据数据源类型选择HTTP或Browser）
     */
    private FetchResult fetchPage(String url, CrawlerSource source) {
        log.info("Fetching page: {}, fetchMode={}", url, source.getFetchMode());
        FetchResult result;
        if ("browser".equalsIgnoreCase(source.getFetchMode())) {
            String waitSelector = source.getWaitSelector();
            Integer waitMs = source.getWaitAfterLoadMs();
            result = browserFetcher.fetch(url, waitSelector, waitMs != null ? waitMs : 2000);
        } else {
            result = httpFetcher.fetch(url);
        }
        log.info("Fetch result: {} -> status={}, htmlLen={}, bodyLen={}, error={}",
                url, result.getStatusCode(),
                result.getHtml() != null ? result.getHtml().length() : "null",
                result.getBody() != null ? result.getBody().length : "null",
                result.getError());
        return result;
    }

    /**
     * 构建数据源配置Map
     */
    private Map<String, String> buildConfig(CrawlerSource source) {
        Map<String, String> config = new HashMap<>();
        if (source == null) return config;

        if (StringUtils.isNotBlank(source.getCrawlStrategy())) {
            config.put("type", source.getCrawlStrategy());
        }
        if (StringUtils.isNotBlank(source.getIncludeRules())) {
            config.put("includePattern", source.getIncludeRules());
        }
        if (StringUtils.isNotBlank(source.getExcludeRules())) {
            config.put("excludePattern", source.getExcludeRules());
        }
        if (StringUtils.isNotBlank(source.getTitleSelector())) {
            config.put("titleSelector", source.getTitleSelector());
        }
        if (StringUtils.isNotBlank(source.getContentSelector())) {
            config.put("contentSelector", source.getContentSelector());
        }
        if (StringUtils.isNotBlank(source.getDateSelector())) {
            config.put("dateSelector", source.getDateSelector());
        }
        if (StringUtils.isNotBlank(source.getAuthorSelector())) {
            config.put("authorSelector", source.getAuthorSelector());
        }
        if (StringUtils.isNotBlank(source.getBaseUrl())) {
            config.put("baseUrl", source.getBaseUrl());
        }
        return config;
    }

    /**
     * 选择适配器
     */
    private CrawlerAdapter selectAdapter(String url, CrawlerSource source, Map<String, String> config) {
        String sourceType = source.getCrawlStrategy();
        if (StringUtils.isNotBlank(sourceType)) {
            CrawlerAdapter adapter = adapterRegistry.get(sourceType.toUpperCase());
            if (adapter != null && adapter.supports(url, config)) {
                return adapter;
            }
            // 尝试小写
            adapter = adapterRegistry.get(sourceType.toLowerCase());
            if (adapter != null && adapter.supports(url, config)) {
                return adapter;
            }
        }
        return genericWebsiteAdapter;
    }

    /**
     * 处理解析后的数据
     */
    private void processParsedData(ParsedData data, String url, CrawlerSource source,
                                    CrawlerTask task, int depth, CrawlTaskContext ctx) {
        String contentType = data.getType();

        // 内容去重检查
        String contentText = TextCleanUtils.htmlToText(
                data.getContentHtml() != null ? data.getContentHtml() : data.getContent());
        String contentHash = HashUtils.sha256(contentText);
        log.info("Content dedup check: url={}, contentHash={}, contentLength={}, title={}",
                url, contentHash, contentText != null ? contentText.length() : 0,
                data.getTitle() != null ? data.getTitle().substring(0, Math.min(30, data.getTitle().length())) : "null");

        // 使用DeduplicationService进行去重
        String dupResult = deduplicationService.checkDuplicate(url, contentText);
        if (dupResult != null) {
            log.info("Duplicate content detected: type={}, url={}, contentHash={}", dupResult, url, contentHash);
            ctx.incrementArticlesDuplicate();
            return;
        }

        // 品牌匹配
        BrandMatchService.MatchResult brandMatch = brandMatchService.matchBest(
                data.getTitle(), data.getContent(), url);

        // 根据内容类型存储
        if ("product".equals(contentType)) {
            storeProduct(data, url, source, task, brandMatch, contentHash);
            ctx.incrementProductsNew();
        } else {
            // 默认作为文章处理
            storeArticle(data, url, source, task, brandMatch, contentHash);
            ctx.incrementArticlesNew();
        }
    }

    /**
     * 存储文章
     */
    private void storeArticle(ParsedData data, String url, CrawlerSource source,
                              CrawlerTask task, BrandMatchService.MatchResult brandMatch, String contentHash) {
        CrawlerArticle article = new CrawlerArticle();
        article.setSourceId(source.getId());
        article.setTaskId(task.getId());
        article.setSourceUrl(url);
        article.setSourceName(source.getSourceName());
        article.setSourceSite(UrlNormalizer.getDomain(url));
        article.setTitle(StringUtils.isNotBlank(data.getTitle()) ? data.getTitle() : UrlNormalizer.getDomain(url) + " - " + url.hashCode());
        article.setContentText(data.getContent());
        article.setContentHtml(HtmlSanitizer.sanitizeWithEnhancements(data.getContentHtml()));
        article.setSummary(data.getSummary());
        article.setAuthor(data.getAuthor());
        article.setPublishTime(parseDateTime(data.getPublishDate()));
        article.setContentHash(contentHash);
        article.setArticleStatus("PENDING_REVIEW");
        article.setCrawlTime(LocalDateTime.now());

        if (brandMatch != null) {
            article.setBrandId(brandMatch.brandId);
        }

        if (data.getTags() != null && !data.getTags().isEmpty()) {
            article.setTags(String.join(",", data.getTags()));
        }

        if (data.getImages() != null && !data.getImages().isEmpty()) {
            article.setCoverImage(data.getImages().get(0));
        }

        storageService.saveArticle(article);
        log.info("Saved article: title={}, brandId={}", article.getTitle(), article.getBrandId());
    }

    /**
     * 存储产品
     */
    private void storeProduct(ParsedData data, String url, CrawlerSource source,
                              CrawlerTask task, BrandMatchService.MatchResult brandMatch, String contentHash) {
        CrawlerProduct product = new CrawlerProduct();
        product.setSourceId(source.getId());
        product.setTaskId(task.getId());
        product.setSourceUrl(url);
        product.setSourceName(source.getSourceName());
        product.setSourceSite(UrlNormalizer.getDomain(url));
        product.setProductName(StringUtils.isNotBlank(data.getTitle()) ? data.getTitle() : UrlNormalizer.getDomain(url) + " - " + url.hashCode());
        product.setDescription(data.getContentHtml());
        product.setContentHash(contentHash);
        product.setProductStatus("PENDING_REVIEW");
        product.setCrawlTime(LocalDateTime.now());

        if (brandMatch != null) {
            product.setBrandId(brandMatch.brandId);
            product.setBrandName(brandMatch.brandName);
        }

        // 参数标准化
        if (data.getParams() != null && !data.getParams().isEmpty()) {
            Map<String, String> normalized = paramNormalizeService.normalizeParams(data.getParams());
            product.setNormalizedParams(toJsonString(normalized));
            product.setRawParams(toJsonString(data.getParams()));
        }

        if (data.getImages() != null && !data.getImages().isEmpty()) {
            product.setCoverImage(data.getImages().get(0));
            product.setGallery(String.join(",", data.getImages()));
        }

        storageService.saveProduct(product);
        log.info("Saved product: name={}, brand={}", product.getProductName(), product.getBrandName());
    }

    /**
     * 判断是否应该跟踪链接（域名检查）
     */
    private boolean shouldFollowDomain(String url, CrawlerSource source) {
        if (StringUtils.isBlank(url)) return false;

        // 同域检查
        if (source.getFollowExternal() == null || source.getFollowExternal() != 1) {
            if (StringUtils.isNotBlank(source.getBaseUrl()) &&
                    !UrlNormalizer.isSameDomain(url, source.getBaseUrl())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 记录错误（持久化到crawler_error表，便于后续分析和重试）
     */
    private void recordError(String url, Long taskId, Long sourceId, String errorType, String errorMessage) {
        log.warn("Crawl error: type={}, url={}, msg={}", errorType, url, errorMessage);
        try {
            CrawlerError error = new CrawlerError();
            error.setSourceId(sourceId);
            error.setTaskId(taskId);
            error.setUrl(url);
            error.setErrorType(errorType);
            error.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                    ? errorMessage.substring(0, 500) : errorMessage);
            error.setRetryCount(0);
            error.setResolved(0);
            errorMapper.insert(error);
        } catch (Exception e) {
            log.warn("Failed to persist crawl error: {}", e.getMessage());
        }
    }

    /**
     * 解析日期字符串为LocalDateTime（使用DateParser增强版）
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (StringUtils.isBlank(dateStr)) return null;
        try {
            java.util.Date date = dateParser.parse(dateStr);
            if (date != null) {
                return date.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String toJsonString(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        return cn.hutool.json.JSONUtil.toJsonStr(map);
    }

    /**
     * URL任务
     */
    public static class UrlTask {
        private final String url;
        private final int depth;
        private final int priority;
        private final Long taskId;

        public UrlTask(String url, int depth, int priority, Long taskId) {
            this.url = url;
            this.depth = depth;
            this.priority = priority;
            this.taskId = taskId;
        }

        public String getUrl() { return url; }
        public int getDepth() { return depth; }
        public int getPriority() { return priority; }
        public Long getTaskId() { return taskId; }
    }
}