package com.robot.home.collector.engine;

import com.robot.home.collector.adapter.CrawlerAdapter;
import com.robot.home.collector.adapter.GenericWebsiteAdapter;
import com.robot.home.collector.adapter.ParsedData;
import com.robot.home.collector.adapter.WeChatAdapter;
import com.robot.home.collector.entity.*;
import com.robot.home.collector.fetcher.BrowserFetcher;
import com.robot.home.collector.fetcher.FetchResult;
import com.robot.home.collector.fetcher.HttpFetcher;
import com.robot.home.collector.mapper.CrawlerTaskMapper;
import com.robot.home.collector.parser.RobotsTxtParser;
import com.robot.home.collector.parser.SitemapParser;
import com.robot.home.collector.service.*;
import com.robot.home.collector.util.HashUtils;
import com.robot.home.collector.util.TextCleanUtils;
import com.robot.home.collector.util.UrlNormalizer;
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
 */
@Component
public class CrawlerEngine {

    private static final Logger log = LoggerFactory.getLogger(CrawlerEngine.class);

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
    private CrawlerTaskMapper taskMapper;

    @Value("${crawler.max-depth:3}")
    private int maxDepth;

    @Value("${crawler.max-urls-per-task:1000}")
    private int maxUrlsPerTask;

    @Value("${crawler.thread-count:3}")
    private int threadCount;

    @Value("${crawler.respect-robots:true}")
    private boolean respectRobots;

    /** 适配器注册表 */
    private final Map<String, CrawlerAdapter> adapterRegistry = new ConcurrentHashMap<>();

    /** URL队列 */
    private PriorityBlockingQueue<UrlTask> urlQueue;

    /** 已访问URL集合 */
    private Set<String> visitedUrls;

    /** 运行状态 */
    private volatile boolean running = false;

    /** 任务锁：防止并发执行多个采集任务（引擎为单例，共享可变状态） */
    private final Object taskLock = new Object();

    /** 工作线程池 */
    private ExecutorService executorService;

    /** 任务计数器 */
    private AtomicInteger urlsDiscovered = new AtomicInteger(0);
    private AtomicInteger urlsSuccess = new AtomicInteger(0);
    private AtomicInteger urlsFailed = new AtomicInteger(0);
    private AtomicInteger articlesNew = new AtomicInteger(0);
    private AtomicInteger articlesUpdated = new AtomicInteger(0);
    private AtomicInteger articlesDuplicate = new AtomicInteger(0);
    private AtomicInteger productsNew = new AtomicInteger(0);
    private AtomicInteger productsUpdated = new AtomicInteger(0);
    private AtomicInteger imagesDownloaded = new AtomicInteger(0);
    private AtomicInteger imagesFailed = new AtomicInteger(0);

    /** 活跃工作线程计数器（用于防止主循环过早退出） */
    private AtomicInteger activeWorkers = new AtomicInteger(0);

    /** 当前任务引用 */
    private volatile CrawlerTask currentTask;

    @PostConstruct
    public void init() {
        // 注册默认适配器
        registerAdapter(genericWebsiteAdapter);
        // 注册微信适配器
        registerAdapter(weChatAdapter);

        // 崩溃恢复：将上次异常退出时遗留的RUNNING任务重置为FAILED，允许重新执行
        try {
            int recovered = taskMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrawlerTask>()
                            .eq(CrawlerTask::getStatus, "RUNNING")
                            .set(CrawlerTask::getStatus, "FAILED")
                            .set(CrawlerTask::getErrorMessage, "Task interrupted by server restart")
                            .set(CrawlerTask::getUpdateTime, LocalDateTime.now())
            );
            if (recovered > 0) {
                log.info("Crash recovery: reset {} RUNNING task(s) to FAILED", recovered);
            }
        } catch (Exception e) {
            log.warn("Crash recovery check failed (non-critical): {}", e.getMessage());
        }
    }

    /**
     * 优雅停机：Spring容器关闭时停止正在运行的采集任务
     */
    @PreDestroy
    public void destroy() {
        if (running) {
            log.info("Shutting down CrawlerEngine gracefully...");
            stop();
            // 将当前任务标记为中断
            if (currentTask != null) {
                try {
                    taskMapper.update(null,
                            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrawlerTask>()
                                    .eq(CrawlerTask::getId, currentTask.getId())
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

    /**
     * 注册适配器
     */
    public void registerAdapter(CrawlerAdapter adapter) {
        adapterRegistry.put(adapter.getType(), adapter);
        log.info("Registered crawler adapter: {}", adapter.getType());
    }

    /**
     * 启动采集任务
     */
    public void startTask(CrawlerSource source, CrawlerTask task) {
        synchronized (taskLock) {
            if (running) {
                throw new IllegalStateException("另一个采集任务正在运行，请等待完成后再启动新任务");
            }
            running = true;
        }

        log.info("Starting crawl task: source={}, task={}, baseUrl={}", source.getSourceName(), task.getId(), source.getBaseUrl());

        // 重置状态（关键：清空上次任务的残留数据）
        this.currentTask = task;
        this.urlQueue = new PriorityBlockingQueue<>(1000,
                Comparator.comparingInt(UrlTask::getPriority).reversed());
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.running = true;

        // 重置计数器
        urlsDiscovered.set(0);
        urlsSuccess.set(0);
        urlsFailed.set(0);
        articlesNew.set(0);
        articlesUpdated.set(0);
        articlesDuplicate.set(0);
        productsNew.set(0);
        productsUpdated.set(0);
        imagesDownloaded.set(0);
        imagesFailed.set(0);
        activeWorkers.set(0);

        // 初始化种子URL
        initializeSeedUrls(source, task);
        log.info("Seed URLs initialized: {} URLs in queue", urlQueue.size());

        if (urlQueue.isEmpty()) {
            log.warn("No seed URLs found for source: {}. Task will complete with 0 results.", source.getSourceName());
            running = false;
            updateTaskCounters(task);
            return;
        }

        // 创建线程池
        executorService = Executors.newFixedThreadPool(threadCount);

        // 启动工作线程
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> workerLoop(source, task));
        }

        // 等待队列清空或任务停止（同时等待活跃工作线程完成，防止过早退出）
        int lastUpdateSize = 0;
        while (running && (!urlQueue.isEmpty() || activeWorkers.get() > 0)) {
            try {
                Thread.sleep(2000);

                // 定期更新任务计数器到数据库
                int currentSize = visitedUrls.size();
                if (currentSize != lastUpdateSize) {
                    updateTaskCounters(task);
                    lastUpdateSize = currentSize;
                    log.info("Crawl progress: visited={}, queue={}, discovered={}, success={}, failed={}",
                            visitedUrls.size(), urlQueue.size(), urlsDiscovered.get(), urlsSuccess.get(), urlsFailed.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 等待工作线程完成当前URL
        stop();

        // 最终更新计数器
        updateTaskCounters(task);
        log.info("Crawl task completed: source={}, urlsVisited={}, discovered={}, success={}, failed={}, articles={}, products={}",
                source.getSourceName(), visitedUrls.size(), urlsDiscovered.get(), urlsSuccess.get(), urlsFailed.get(),
                articlesNew.get(), productsNew.get());
    }

    /**
     * 停止采集
     */
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 更新任务计数器到数据库
     */
    private void updateTaskCounters(CrawlerTask task) {
        try {
            CrawlerTask updateTask = new CrawlerTask();
            updateTask.setId(task.getId());
            updateTask.setUrlsDiscovered(urlsDiscovered.get());
            updateTask.setUrlsSuccess(urlsSuccess.get());
            updateTask.setUrlsFailed(urlsFailed.get());
            updateTask.setArticlesNew(articlesNew.get());
            updateTask.setArticlesUpdated(articlesUpdated.get());
            updateTask.setArticlesDuplicate(articlesDuplicate.get());
            updateTask.setProductsNew(productsNew.get());
            updateTask.setProductsUpdated(productsUpdated.get());
            updateTask.setImagesDownloaded(imagesDownloaded.get());
            updateTask.setImagesFailed(imagesFailed.get());
            updateTask.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(updateTask);
        } catch (Exception e) {
            log.warn("Failed to update task counters: {}", e.getMessage());
        }
    }

    /**
     * 初始化种子URL
     */
    private void initializeSeedUrls(CrawlerSource source, CrawlerTask task) {
        List<String> seedUrls = new ArrayList<>();

        // 从数据源配置获取种子URL
        if (StringUtils.isNotBlank(source.getSeedUrls())) {
            seedUrls.addAll(Arrays.asList(source.getSeedUrls().split("\\n")));
            log.info("Loaded {} seed URLs from source config", seedUrls.size());
        }

        // 从sitemap获取URL
        if (StringUtils.isNotBlank(source.getSitemapUrl())) {
            try {
                FetchResult result = httpFetcher.fetch(source.getSitemapUrl());
                if (result.isSuccess()) {
                    SitemapParser.SitemapResult sitemapResult = sitemapParser.parse(result.getHtml());
                    List<SitemapParser.SitemapUrl> sitemapUrls = sitemapResult.getUrls();
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
                enqueue(normalized, 0, 10, task.getId()); // 种子URL高优先级
                addedCount++;
            }
        }

        urlsDiscovered.addAndGet(addedCount);
        log.info("Initialized {} seed URLs for source: {} (total {} raw seeds)", addedCount, source.getSourceName(), seedUrls.size());
    }

    /**
     * 工作线程循环
     */
    private void workerLoop(CrawlerSource source, CrawlerTask task) {
        while (running) {
            try {
                UrlTask urlTask = urlQueue.poll(5, TimeUnit.SECONDS);
                if (urlTask == null) {
                    if (urlQueue.isEmpty() && activeWorkers.get() == 0) break;
                    continue;
                }

                activeWorkers.incrementAndGet();
                try {
                    processUrl(urlTask, source, task);
                } finally {
                    activeWorkers.decrementAndGet();
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
    private void processUrl(UrlTask urlTask, CrawlerSource source, CrawlerTask task) {
        String url = urlTask.getUrl();
        String urlHash = UrlNormalizer.hash(url);

        // 检查是否已访问
        if (!visitedUrls.add(urlHash)) {
            log.info("URL already visited in this task: {}", url);
            return;
        }

        // 检查robots.txt
        if (respectRobots && !robotsTxtParser.isAllowed(url, "RobotHomeCrawler")) {
            log.warn("Blocked by robots.txt: {}", url);
            urlsFailed.incrementAndGet();
            return;
        }

        // 检查URL去重（跨任务去重）
        if (deduplicationService.isUrlDuplicate(url)) {
            log.info("URL already processed in previous task: {}", url);
            articlesDuplicate.incrementAndGet();
            return;
        }

        // 检查URL数量限制
        if (visitedUrls.size() >= maxUrlsPerTask) {
            log.info("Reached max URLs limit: {}", maxUrlsPerTask);
            running = false;
            return;
        }

        log.info("Processing URL: {} (depth={}, visited={}/{})", url, urlTask.getDepth(), visitedUrls.size(), maxUrlsPerTask);

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
                    log.info("Redirect detected: {} -> {} (HTTP {})", url, redirectUrl, fetchResult.getStatusCode());
                    if (urlTask.getDepth() < maxDepth && shouldFollowDomain(redirectUrl, source)) {
                        enqueue(redirectUrl, urlTask.getDepth(), urlTask.getPriority(), task.getId());
                        urlsDiscovered.incrementAndGet();
                    }
                } else {
                    log.warn("Redirect without Location header: {} -> HTTP {}", url, fetchResult.getStatusCode());
                }
                urlsFailed.incrementAndGet();
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
                urlsFailed.incrementAndGet();
                recordError(url, task.getId(), "FETCH_ERROR", "HTTP " + fetchResult.getStatusCode());
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
                urlsFailed.incrementAndGet();
                // 解析失败，标记为FAILED允许重试
                deduplicationService.markUrlFetched(url, "FAILED");
                return;
            }

            // 5. 处理解析结果
            processParsedData(parsedData, url, source, task, urlTask.getDepth());
            urlsSuccess.incrementAndGet();

            // 6. 提取链接并加入队列
            if (urlTask.getDepth() < maxDepth) {
                List<String> links = adapter.extractLinks(fetchResult, config);
                int newLinksCount = 0;
                for (String link : links) {
                    String normalized = UrlNormalizer.normalize(link);
                    if (adapter.shouldFollow(normalized, urlTask.getDepth() + 1, maxDepth, config)) {
                        if (shouldFollowDomain(normalized, source)) {
                            if (enqueue(normalized, urlTask.getDepth() + 1, 1, task.getId())) {
                                newLinksCount++;
                            }
                        }
                    }
                }
                urlsDiscovered.addAndGet(newLinksCount);
                if (newLinksCount > 0) {
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
            urlsFailed.incrementAndGet();
            recordError(url, task.getId(), "PROCESS_ERROR", e.getMessage());
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
                                    CrawlerTask task, int depth) {
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
            articlesDuplicate.incrementAndGet();
            return;
        }

        // 品牌匹配
        BrandMatchService.MatchResult brandMatch = brandMatchService.matchBest(
                data.getTitle(), data.getContent(), url);

        // 根据内容类型存储
        if ("product".equals(contentType)) {
            storeProduct(data, url, source, task, brandMatch, contentHash);
            productsNew.incrementAndGet();
        } else {
            // 默认作为文章处理
            storeArticle(data, url, source, task, brandMatch, contentHash);
            articlesNew.incrementAndGet();
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
        article.setContentHtml(data.getContentHtml());
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
     * 入队
     * @return true if URL was newly enqueued, false if already visited
     */
    private boolean enqueue(String url, int depth, int priority, Long taskId) {
        String normalized = UrlNormalizer.normalize(url);
        if (StringUtils.isBlank(normalized)) return false;
        if (visitedUrls.contains(UrlNormalizer.hash(normalized))) return false;

        urlQueue.offer(new UrlTask(normalized, depth, priority, taskId));
        return true;
    }

    /**
     * 记录错误
     */
    private void recordError(String url, Long taskId, String errorType, String errorMessage) {
        log.warn("Crawl error: type={}, url={}, msg={}", errorType, url, errorMessage);
    }

    /**
     * 解析日期字符串为LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (StringUtils.isBlank(dateStr)) return null;

        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"),
                DateTimeFormatter.ofPattern("yyyy年MM月dd日"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyyMMdd")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate date = LocalDate.parse(dateStr.trim(), formatter);
                return date.atStartOfDay();
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String toJsonString(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
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