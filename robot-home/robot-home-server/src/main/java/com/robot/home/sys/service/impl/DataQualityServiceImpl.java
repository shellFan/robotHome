package com.robot.home.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.crawler.entity.ServerCrawlerArticle;
import com.robot.home.crawler.entity.ServerCrawlerProduct;
import com.robot.home.crawler.entity.ServerCrawlerSource;
import com.robot.home.crawler.mapper.ServerCrawlerArticleMapper;
import com.robot.home.crawler.mapper.ServerCrawlerProductMapper;
import com.robot.home.crawler.mapper.ServerCrawlerSourceMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotCategory;
import com.robot.home.robot.mapper.RobotCategoryMapper;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.sys.service.DataQualityService;
import com.robot.home.sys.entity.DataQualitySnapshot;
import com.robot.home.sys.mapper.DataQualitySnapshotMapper;
import com.robot.home.sys.vo.CoverageDrillDownVO;
import com.robot.home.sys.vo.DataQualityVO;
import com.robot.home.sys.vo.DataScorecardVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据质量统计服务实现
 * Phase12: 真实数据成绩报告
 */
@Service
public class DataQualityServiceImpl implements DataQualityService {

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private RobotCategoryMapper robotCategoryMapper;
    @Resource
    private ServerCrawlerProductMapper crawlerProductMapper;
    @Resource
    private ServerCrawlerArticleMapper crawlerArticleMapper;
    @Resource
    private ServerCrawlerSourceMapper crawlerSourceMapper;
    @Resource
    private DataQualitySnapshotMapper snapshotMapper;

    @Override
    public DataQualityVO getDataQuality() {
        DataQualityVO vo = new DataQualityVO();
        computeRobotQuality(vo);
        computeBrandQuality(vo);
        computeArticleQuality(vo);
        computeCrawlerStats(vo);
        computeSourceHealth(vo);
        computeFreshnessStats(vo);
        return vo;
    }

    private void computeRobotQuality(DataQualityVO vo) {
        LambdaQueryWrapper<Robot> all = Wrappers.<Robot>lambdaQuery();
        long total = robotMapper.selectCount(all);
        vo.setRobotTotal(total);

        // 封面图覆盖率
        long withCover = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .isNotNull(Robot::getCoverImage)
                .ne(Robot::getCoverImage, ""));
        vo.setRobotWithCoverImage(withCover);
        vo.setRobotCoverImageRate(total > 0 ? round2(withCover * 100.0 / total) : 0.0);

        // 图集覆盖率
        long withGallery = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .isNotNull(Robot::getImages)
                .ne(Robot::getImages, ""));
        vo.setRobotWithGallery(withGallery);
        vo.setRobotGalleryRate(total > 0 ? round2(withGallery * 100.0 / total) : 0.0);

        // 参数覆盖率
        long withParams = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .isNotNull(Robot::getMainParams)
                .ne(Robot::getMainParams, ""));
        vo.setRobotWithParams(withParams);
        vo.setRobotParamRate(total > 0 ? round2(withParams * 100.0 / total) : 0.0);

        // 来源URL覆盖率
        long withSourceUrl = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .isNotNull(Robot::getSourceUrl)
                .ne(Robot::getSourceUrl, ""));
        vo.setRobotWithSourceUrl(withSourceUrl);
        vo.setRobotSourceUrlRate(total > 0 ? round2(withSourceUrl * 100.0 / total) : 0.0);

        // 品牌覆盖率
        long withBrand = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .isNotNull(Robot::getBrandId)
                .gt(Robot::getBrandId, 0));
        vo.setRobotWithBrand(withBrand);
        vo.setRobotBrandRate(total > 0 ? round2(withBrand * 100.0 / total) : 0.0);

        // 分类覆盖率
        long withCategory = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .isNotNull(Robot::getCategoryId)
                .gt(Robot::getCategoryId, 0));
        vo.setRobotWithCategory(withCategory);
        vo.setRobotCategoryRate(total > 0 ? round2(withCategory * 100.0 / total) : 0.0);

        // 按dataSource分组
        Map<String, Long> bySource = new HashMap<>();
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .select(Robot::getId, Robot::getDataSource));
        for (Robot r : robots) {
            String ds = r.getDataSource() != null ? r.getDataSource() : "UNKNOWN";
            bySource.merge(ds, 1L, Long::sum);
        }
        vo.setRobotByDataSource(bySource);
    }

    private void computeBrandQuality(DataQualityVO vo) {
        long total = brandMapper.selectCount(Wrappers.<Brand>lambdaQuery());
        vo.setBrandTotal(total);

        long withWebsite = brandMapper.selectCount(Wrappers.<Brand>lambdaQuery()
                .isNotNull(Brand::getWebsite)
                .ne(Brand::getWebsite, ""));
        vo.setBrandWithWebsite(withWebsite);
        vo.setBrandWebsiteRate(total > 0 ? round2(withWebsite * 100.0 / total) : 0.0);
    }

    private void computeArticleQuality(DataQualityVO vo) {
        long total = articleMapper.selectCount(Wrappers.<Article>lambdaQuery());
        vo.setArticleTotal(total);

        // Article用cover字段(非coverImage)
        long withCover = articleMapper.selectCount(Wrappers.<Article>lambdaQuery()
                .isNotNull(Article::getCover)
                .ne(Article::getCover, ""));
        vo.setArticleWithCoverImage(withCover);
        vo.setArticleCoverImageRate(total > 0 ? round2(withCover * 100.0 / total) : 0.0);

        // Article无sourceUrl字段，用source字段替代
        long withSourceUrl = articleMapper.selectCount(Wrappers.<Article>lambdaQuery()
                .isNotNull(Article::getSource)
                .ne(Article::getSource, ""));
        vo.setArticleWithSourceUrl(withSourceUrl);
        vo.setArticleSourceUrlRate(total > 0 ? round2(withSourceUrl * 100.0 / total) : 0.0);
    }

    private void computeCrawlerStats(DataQualityVO vo) {
        // 采集产品统计
        long productTotal = crawlerProductMapper.selectCount(Wrappers.<ServerCrawlerProduct>lambdaQuery());
        vo.setCrawlerProductTotal(productTotal);

        long productPending = crawlerProductMapper.selectCount(Wrappers.<ServerCrawlerProduct>lambdaQuery()
                .eq(ServerCrawlerProduct::getProductStatus, "PENDING_REVIEW"));
        vo.setCrawlerProductPendingReview(productPending);

        long productPublished = crawlerProductMapper.selectCount(Wrappers.<ServerCrawlerProduct>lambdaQuery()
                .eq(ServerCrawlerProduct::getProductStatus, "PUBLISHED"));
        vo.setCrawlerProductPublished(productPublished);

        long productDuplicate = crawlerProductMapper.selectCount(Wrappers.<ServerCrawlerProduct>lambdaQuery()
                .eq(ServerCrawlerProduct::getProductStatus, "DUPLICATE"));
        vo.setCrawlerProductDuplicate(productDuplicate);

        // 采集文章统计
        long articleTotal = crawlerArticleMapper.selectCount(Wrappers.<ServerCrawlerArticle>lambdaQuery());
        vo.setCrawlerArticleTotal(articleTotal);

        long articlePending = crawlerArticleMapper.selectCount(Wrappers.<ServerCrawlerArticle>lambdaQuery()
                .eq(ServerCrawlerArticle::getArticleStatus, "PENDING_REVIEW"));
        vo.setCrawlerArticlePendingReview(articlePending);

        long articlePublished = crawlerArticleMapper.selectCount(Wrappers.<ServerCrawlerArticle>lambdaQuery()
                .eq(ServerCrawlerArticle::getArticleStatus, "PUBLISHED"));
        vo.setCrawlerArticlePublished(articlePublished);

        long articleDuplicate = crawlerArticleMapper.selectCount(Wrappers.<ServerCrawlerArticle>lambdaQuery()
                .eq(ServerCrawlerArticle::getArticleStatus, "DUPLICATE"));
        vo.setCrawlerArticleDuplicate(articleDuplicate);
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    /**
     * Phase12: Source健康度统计
     */
    private void computeSourceHealth(DataQualityVO vo) {
        long total = crawlerSourceMapper.selectCount(Wrappers.<ServerCrawlerSource>lambdaQuery());
        vo.setSourceTotal(total);

        // 按healthStatus分组
        vo.setSourceHealthy(countSourceByHealth("HEALTHY"));
        vo.setSourceDegraded(countSourceByHealth("DEGRADED"));
        vo.setSourceFailed(countSourceByHealth("FAILED"));
        vo.setSourceDisabled(countSourceByHealth("DISABLED"));
        vo.setSourceUnknown(countSourceByHealth("UNKNOWN"));

        // 按trustLevel分组
        vo.setSourceOfficial(countSourceByTrust("OFFICIAL"));
        vo.setSourceTrusted(countSourceByTrust("TRUSTED"));
        vo.setSourceNormal(countSourceByTrust("NORMAL"));
    }

    private long countSourceByHealth(String status) {
        return crawlerSourceMapper.selectCount(Wrappers.<ServerCrawlerSource>lambdaQuery()
                .eq(ServerCrawlerSource::getHealthStatus, status));
    }

    private long countSourceByTrust(String level) {
        return crawlerSourceMapper.selectCount(Wrappers.<ServerCrawlerSource>lambdaQuery()
                .eq(ServerCrawlerSource::getTrustLevel, level));
    }

    /**
     * Phase12: Robot Freshness统计
     */
    private void computeFreshnessStats(DataQualityVO vo) {
        vo.setRobotFresh(robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getFreshness, "FRESH")));
        vo.setRobotAging(robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getFreshness, "AGING")));
        vo.setRobotStale(robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getFreshness, "STALE")));
    }

    /**
     * Phase12 P0-5: Coverage下钻统计
     */
    @Override
    public CoverageDrillDownVO getCoverageDrillDown(String dimension) {
        CoverageDrillDownVO vo = new CoverageDrillDownVO();

        // 维度参数校验: 只允许dataSource/brand/category
        if (dimension == null) {
            dimension = "dataSource";
        }
        if (!"dataSource".equals(dimension) && !"brand".equals(dimension) && !"category".equals(dimension)) {
            throw new IllegalArgumentException("Invalid dimension: " + dimension + ". Allowed: dataSource, brand, category");
        }
        vo.setDimension(dimension);

        // 查询所有Robot的覆盖维度字段
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .select(Robot::getId, Robot::getDataSource, Robot::getBrandId, Robot::getCategoryId,
                        Robot::getCoverImage, Robot::getImages, Robot::getMainParams, Robot::getSourceUrl));

        Map<String, List<Robot>> grouped;
        switch (dimension) {
            case "brand":
                grouped = groupByBrand(robots);
                break;
            case "category":
                grouped = groupByCategory(robots);
                break;
            case "dataSource":
            default:
                grouped = robots.stream()
                        .collect(Collectors.groupingBy(r -> r.getDataSource() != null ? r.getDataSource() : "UNKNOWN"));
                break;
        }

        List<CoverageDrillDownVO.DimensionItem> items = new ArrayList<>();
        for (Map.Entry<String, List<Robot>> entry : grouped.entrySet()) {
            List<Robot> group = entry.getValue();
            long total = group.size();
            long withCover = group.stream().filter(r -> r.getCoverImage() != null && !r.getCoverImage().isEmpty()).count();
            long withGallery = group.stream().filter(r -> r.getImages() != null && !r.getImages().isEmpty()).count();
            long withParams = group.stream().filter(r -> r.getMainParams() != null && !r.getMainParams().isEmpty()).count();
            long withSourceUrl = group.stream().filter(r -> r.getSourceUrl() != null && !r.getSourceUrl().isEmpty()).count();

            CoverageDrillDownVO.DimensionItem item = new CoverageDrillDownVO.DimensionItem();
            item.setName(entry.getKey());
            item.setTotal(total);
            item.setWithCoverImage(withCover);
            item.setCoverImageRate(total > 0 ? round2(withCover * 100.0 / total) : 0.0);
            item.setWithGallery(withGallery);
            item.setGalleryRate(total > 0 ? round2(withGallery * 100.0 / total) : 0.0);
            item.setWithParams(withParams);
            item.setParamRate(total > 0 ? round2(withParams * 100.0 / total) : 0.0);
            item.setWithSourceUrl(withSourceUrl);
            item.setSourceUrlRate(total > 0 ? round2(withSourceUrl * 100.0 / total) : 0.0);
            items.add(item);
        }

        // 按total降序排列
        items.sort((a, b) -> Long.compare(b.getTotal(), a.getTotal()));
        vo.setItems(items);
        return vo;
    }

    /**
     * 按品牌分组(brandId -> brandName)
     */
    private Map<String, List<Robot>> groupByBrand(List<Robot> robots) {
        // 先按brandId分组
        Map<Long, List<Robot>> byBrandId = robots.stream()
                .filter(r -> r.getBrandId() != null && r.getBrandId() > 0)
                .collect(Collectors.groupingBy(Robot::getBrandId));

        // 查询品牌名称映射
        Map<Long, String> brandNames = new HashMap<>();
        if (!byBrandId.isEmpty()) {
            List<Brand> brands = brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                    .in(Brand::getId, byBrandId.keySet())
                    .select(Brand::getId, Brand::getName));
            for (Brand b : brands) {
                brandNames.put(b.getId(), b.getName());
            }
        }

        // 转换为brandName分组
        Map<String, List<Robot>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Robot>> entry : byBrandId.entrySet()) {
            String name = brandNames.getOrDefault(entry.getKey(), "brand_" + entry.getKey());
            result.put(name, entry.getValue());
        }

        // 无品牌的Robot归入UNKNOWN
        List<Robot> noBrand = robots.stream()
                .filter(r -> r.getBrandId() == null || r.getBrandId() <= 0)
                .collect(Collectors.toList());
        if (!noBrand.isEmpty()) {
            result.put("UNKNOWN", noBrand);
        }

        return result;
    }

    /**
     * 按分类分组(categoryId -> categoryName)
     */
    private Map<String, List<Robot>> groupByCategory(List<Robot> robots) {
        // 先按categoryId分组
        Map<Long, List<Robot>> byCategoryId = robots.stream()
                .filter(r -> r.getCategoryId() != null && r.getCategoryId() > 0)
                .collect(Collectors.groupingBy(Robot::getCategoryId));

        // 查询分类名称映射
        Map<Long, String> categoryNames = new HashMap<>();
        if (!byCategoryId.isEmpty()) {
            List<RobotCategory> categories = robotCategoryMapper.selectList(Wrappers.<RobotCategory>lambdaQuery()
                    .in(RobotCategory::getId, byCategoryId.keySet())
                    .select(RobotCategory::getId, RobotCategory::getName));
            for (RobotCategory c : categories) {
                categoryNames.put(c.getId(), c.getName());
            }
        }

        // 转换为categoryName分组
        Map<String, List<Robot>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Robot>> entry : byCategoryId.entrySet()) {
            String name = categoryNames.getOrDefault(entry.getKey(), "category_" + entry.getKey());
            result.put(name, entry.getValue());
        }

        // 无分类的Robot归入UNKNOWN
        List<Robot> noCategory = robots.stream()
                .filter(r -> r.getCategoryId() == null || r.getCategoryId() <= 0)
                .collect(Collectors.toList());
        if (!noCategory.isEmpty()) {
            result.put("UNKNOWN", noCategory);
        }

        return result;
    }

    /**
     * Phase12 P0-6: 数据Scorecard — 持久化快照(BEFORE/AFTER不可变)
     * capture=true: 从当前DB计算并持久化到snapshot表
     * capture=false: 从snapshot表读取最近一次该label的快照
     */
    @Override
    public DataScorecardVO getScorecard(String label) {
        // 从当前DB计算
        DataQualityVO dq = getDataQuality();

        DataScorecardVO sc = new DataScorecardVO();
        sc.setSnapshotTime(LocalDateTime.now());
        sc.setLabel(label != null ? label : "SNAPSHOT");

        // Robot Coverage
        sc.setRobotTotal(dq.getRobotTotal());
        sc.setRobotCoverImageRate(dq.getRobotCoverImageRate());
        sc.setRobotGalleryRate(dq.getRobotGalleryRate());
        sc.setRobotParamRate(dq.getRobotParamRate());
        sc.setRobotSourceUrlRate(dq.getRobotSourceUrlRate());
        sc.setRobotBrandRate(dq.getRobotBrandRate());
        sc.setRobotCategoryRate(dq.getRobotCategoryRate());

        // Source Health
        sc.setSourceTotal(dq.getSourceTotal());
        sc.setSourceHealthy(dq.getSourceHealthy());
        sc.setSourceDegraded(dq.getSourceDegraded());
        sc.setSourceFailed(dq.getSourceFailed());
        sc.setSourceUnknown(dq.getSourceUnknown());

        // Freshness
        sc.setRobotFresh(dq.getRobotFresh());
        sc.setRobotAging(dq.getRobotAging());
        sc.setRobotStale(dq.getRobotStale());

        // Brand & Article
        sc.setBrandTotal(dq.getBrandTotal());
        sc.setBrandWebsiteRate(dq.getBrandWebsiteRate());
        sc.setArticleTotal(dq.getArticleTotal());
        sc.setArticleCoverImageRate(dq.getArticleCoverImageRate());
        sc.setArticleSourceUrlRate(dq.getArticleSourceUrlRate());

        // Crawler Pipeline
        sc.setCrawlerProductTotal(dq.getCrawlerProductTotal());
        sc.setCrawlerProductPendingReview(dq.getCrawlerProductPendingReview());
        sc.setCrawlerProductPublished(dq.getCrawlerProductPublished());
        sc.setCrawlerArticleTotal(dq.getCrawlerArticleTotal());
        sc.setCrawlerArticlePendingReview(dq.getCrawlerArticlePendingReview());
        sc.setCrawlerArticlePublished(dq.getCrawlerArticlePublished());

        // 计算综合评分
        sc.computeOverallScore();

        // 持久化到snapshot表(不可变快照)
        DataQualitySnapshot snapshot = toSnapshot(sc);
        snapshotMapper.insert(snapshot);

        return sc;
    }

    /**
     * 从snapshot表读取最近一次指定label的快照(不可变)
     */
    @Override
    public DataScorecardVO getScorecardSnapshot(String label) {
        DataQualitySnapshot snapshot = snapshotMapper.selectOne(
                Wrappers.<DataQualitySnapshot>lambdaQuery()
                        .eq(DataQualitySnapshot::getLabel, label)
                        .orderByDesc(DataQualitySnapshot::getSnapshotTime)
                        .last("LIMIT 1"));
        if (snapshot == null) {
            return null;
        }
        return toScorecardVO(snapshot);
    }

    /**
     * ScorecardVO → Snapshot Entity
     */
    private DataQualitySnapshot toSnapshot(DataScorecardVO sc) {
        DataQualitySnapshot s = new DataQualitySnapshot();
        s.setLabel(sc.getLabel());
        s.setSnapshotTime(sc.getSnapshotTime());
        s.setEnvironment("STAGING"); // 默认STAGING，生产环境可配置
        s.setRobotTotal(sc.getRobotTotal());
        s.setRobotCoverImageRate(sc.getRobotCoverImageRate());
        s.setRobotGalleryRate(sc.getRobotGalleryRate());
        s.setRobotParamRate(sc.getRobotParamRate());
        s.setRobotSourceUrlRate(sc.getRobotSourceUrlRate());
        s.setRobotBrandRate(sc.getRobotBrandRate());
        s.setRobotCategoryRate(sc.getRobotCategoryRate());
        s.setSourceTotal(sc.getSourceTotal());
        s.setSourceHealthy(sc.getSourceHealthy());
        s.setSourceDegraded(sc.getSourceDegraded());
        s.setSourceFailed(sc.getSourceFailed());
        s.setSourceUnknown(sc.getSourceUnknown());
        s.setRobotFresh(sc.getRobotFresh());
        s.setRobotAging(sc.getRobotAging());
        s.setRobotStale(sc.getRobotStale());
        s.setBrandTotal(sc.getBrandTotal());
        s.setBrandWebsiteRate(sc.getBrandWebsiteRate());
        s.setArticleTotal(sc.getArticleTotal());
        s.setArticleCoverImageRate(sc.getArticleCoverImageRate());
        s.setArticleSourceUrlRate(sc.getArticleSourceUrlRate());
        s.setCrawlerProductTotal(sc.getCrawlerProductTotal());
        s.setCrawlerProductPendingReview(sc.getCrawlerProductPendingReview());
        s.setCrawlerProductPublished(sc.getCrawlerProductPublished());
        s.setCrawlerArticleTotal(sc.getCrawlerArticleTotal());
        s.setCrawlerArticlePendingReview(sc.getCrawlerArticlePendingReview());
        s.setCrawlerArticlePublished(sc.getCrawlerArticlePublished());
        s.setOverallScore(sc.getOverallScore());
        return s;
    }

    /**
     * Snapshot Entity → ScorecardVO
     */
    private DataScorecardVO toScorecardVO(DataQualitySnapshot s) {
        DataScorecardVO sc = new DataScorecardVO();
        sc.setSnapshotTime(s.getSnapshotTime());
        sc.setLabel(s.getLabel());
        sc.setRobotTotal(s.getRobotTotal());
        sc.setRobotCoverImageRate(s.getRobotCoverImageRate());
        sc.setRobotGalleryRate(s.getRobotGalleryRate());
        sc.setRobotParamRate(s.getRobotParamRate());
        sc.setRobotSourceUrlRate(s.getRobotSourceUrlRate());
        sc.setRobotBrandRate(s.getRobotBrandRate());
        sc.setRobotCategoryRate(s.getRobotCategoryRate());
        sc.setSourceTotal(s.getSourceTotal());
        sc.setSourceHealthy(s.getSourceHealthy());
        sc.setSourceDegraded(s.getSourceDegraded());
        sc.setSourceFailed(s.getSourceFailed());
        sc.setSourceUnknown(s.getSourceUnknown());
        sc.setRobotFresh(s.getRobotFresh());
        sc.setRobotAging(s.getRobotAging());
        sc.setRobotStale(s.getRobotStale());
        sc.setBrandTotal(s.getBrandTotal());
        sc.setBrandWebsiteRate(s.getBrandWebsiteRate());
        sc.setArticleTotal(s.getArticleTotal());
        sc.setArticleCoverImageRate(s.getArticleCoverImageRate());
        sc.setArticleSourceUrlRate(s.getArticleSourceUrlRate());
        sc.setCrawlerProductTotal(s.getCrawlerProductTotal());
        sc.setCrawlerProductPendingReview(s.getCrawlerProductPendingReview());
        sc.setCrawlerProductPublished(s.getCrawlerProductPublished());
        sc.setCrawlerArticleTotal(s.getCrawlerArticleTotal());
        sc.setCrawlerArticlePendingReview(s.getCrawlerArticlePendingReview());
        sc.setCrawlerArticlePublished(s.getCrawlerArticlePublished());
        sc.setOverallScore(s.getOverallScore());
        return sc;
    }
}