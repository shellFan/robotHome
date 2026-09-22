package com.robot.home.growth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.growth.entity.GrowthDailyStat;
import com.robot.home.growth.mapper.GrowthDailyStatMapper;
import com.robot.home.growth.service.GrowthService;
import com.robot.home.growth.vo.EcosystemDashboardVO;
import com.robot.home.growth.vo.GrowthDashboardVO;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.reputation.entity.UserReputation;
import com.robot.home.reputation.mapper.UserReputationMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.subscription.entity.UserSubscription;
import com.robot.home.subscription.mapper.UserSubscriptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Phase9: 增长分析服务实现
 * 优先从growth_daily_stat日快照读取
 * Redis缓存+DB fallback
 */
@Service
public class GrowthServiceImpl implements GrowthService {

    private static final Logger log = LoggerFactory.getLogger(GrowthServiceImpl.class);

    private static final long CACHE_SECONDS = 300L;

    @Resource
    private GrowthDailyStatMapper growthDailyStatMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private UserReputationMapper userReputationMapper;
    @Resource
    private InquiryMapper inquiryMapper;
    @Resource
    private UserSubscriptionMapper userSubscriptionMapper;

    @Override
    public GrowthDashboardVO dashboard(int days) {
        int trendDays = Math.max(7, Math.min(days, 30));
        String cacheKey = Constants.CACHE_GROWTH_PREFIX + "dashboard:" + trendDays;
        try {
            GrowthDashboardVO cached = redisUtils.getObj(cacheKey, GrowthDashboardVO.class);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis增长Dashboard缓存读取失败，降级到DB: error={}", e.getMessage());
        }

        GrowthDashboardVO vo = new GrowthDashboardVO();

        // 今日概览
        LocalDate today = LocalDate.now();
        GrowthDailyStat todayStat = growthDailyStatMapper.selectOne(Wrappers.<GrowthDailyStat>lambdaQuery()
                .eq(GrowthDailyStat::getStatDate, today));
        vo.setToday(toTodayStats(todayStat));

        // 趋势
        List<GrowthDailyStat> stats = growthDailyStatMapper.selectList(Wrappers.<GrowthDailyStat>lambdaQuery()
                .ge(GrowthDailyStat::getStatDate, today.minusDays(trendDays - 1))
                .orderByAsc(GrowthDailyStat::getStatDate));
        List<GrowthDashboardVO.DailyStatItem> trend = new ArrayList<>();
        for (GrowthDailyStat s : stats) {
            trend.add(toDailyStatItem(s));
        }
        if (trendDays <= 7) {
            vo.setTrend7d(trend);
        } else {
            vo.setTrend30d(trend);
        }

        // 转化漏斗
        vo.setFunnel(buildFunnel(todayStat));

        // Top热门机器人
        List<Robot> topRobots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getStatus, 1)
                .orderByDesc(Robot::getViewCount)
                .last("LIMIT 10"));
        List<GrowthDashboardVO.TopItem> topRobotItems = new ArrayList<>();
        for (Robot r : topRobots) {
            GrowthDashboardVO.TopItem item = new GrowthDashboardVO.TopItem();
            item.setId(r.getId());
            item.setName(r.getName());
            item.setCount(r.getViewCount());
            topRobotItems.add(item);
        }
        vo.setTopRobots(topRobotItems);

        // Top热门品牌
        List<Brand> topBrands = brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByDesc(Brand::getHotScore)
                .last("LIMIT 10"));
        List<GrowthDashboardVO.TopItem> topBrandItems = new ArrayList<>();
        for (Brand b : topBrands) {
            GrowthDashboardVO.TopItem item = new GrowthDashboardVO.TopItem();
            item.setId(b.getId());
            item.setName(b.getName());
            item.setCount(b.getRobotCount());
            topBrandItems.add(item);
        }
        vo.setTopBrands(topBrandItems);

        // 缓存
        try {
            redisUtils.setObj(cacheKey, vo, CACHE_SECONDS + (long) (Math.random() * 30), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis增长Dashboard缓存写入失败（不影响返回）: error={}", e.getMessage());
        }

        return vo;
    }

    private GrowthDashboardVO.TodayStats toTodayStats(GrowthDailyStat stat) {
        GrowthDashboardVO.TodayStats today = new GrowthDashboardVO.TodayStats();
        if (stat == null) {
            today.setDau(0);
            today.setNewUsers(0);
            today.setRobotViews(0);
            today.setSearches(0);
            today.setFavorites(0);
            today.setCompares(0);
            today.setQuestions(0);
            today.setAnswers(0);
            today.setPosts(0);
            today.setReviews(0);
            today.setInquiries(0);
            return today;
        }
        today.setDau(stat.getDau() == null ? 0 : stat.getDau());
        today.setNewUsers(stat.getNewUsers() == null ? 0 : stat.getNewUsers());
        today.setRobotViews(stat.getRobotViews() == null ? 0 : stat.getRobotViews());
        today.setSearches(stat.getSearches() == null ? 0 : stat.getSearches());
        today.setFavorites(stat.getFavorites() == null ? 0 : stat.getFavorites());
        today.setCompares(stat.getCompares() == null ? 0 : stat.getCompares());
        today.setQuestions(stat.getQuestions() == null ? 0 : stat.getQuestions());
        today.setAnswers(stat.getAnswers() == null ? 0 : stat.getAnswers());
        today.setPosts(stat.getPosts() == null ? 0 : stat.getPosts());
        today.setReviews(stat.getReviews() == null ? 0 : stat.getReviews());
        today.setInquiries(stat.getInquiries() == null ? 0 : stat.getInquiries());
        return today;
    }

    private GrowthDashboardVO.DailyStatItem toDailyStatItem(GrowthDailyStat s) {
        GrowthDashboardVO.DailyStatItem item = new GrowthDashboardVO.DailyStatItem();
        item.setDate(s.getStatDate() == null ? null : s.getStatDate().toString());
        item.setDau(s.getDau() == null ? 0 : s.getDau());
        item.setNewUsers(s.getNewUsers() == null ? 0 : s.getNewUsers());
        item.setRobotViews(s.getRobotViews() == null ? 0 : s.getRobotViews());
        item.setSearches(s.getSearches() == null ? 0 : s.getSearches());
        item.setFavorites(s.getFavorites() == null ? 0 : s.getFavorites());
        item.setQuestions(s.getQuestions() == null ? 0 : s.getQuestions());
        item.setPosts(s.getPosts() == null ? 0 : s.getPosts());
        return item;
    }

    private GrowthDashboardVO.FunnelItem buildFunnel(GrowthDailyStat stat) {
        GrowthDashboardVO.FunnelItem funnel = new GrowthDashboardVO.FunnelItem();
        if (stat == null) {
            funnel.setViews(0);
            funnel.setEngages(0);
            funnel.setSelections(0);
            funnel.setInquiries(0);
            return funnel;
        }
        funnel.setViews(stat.getRobotViews() == null ? 0 : stat.getRobotViews());
        int engages = (stat.getFavorites() == null ? 0 : stat.getFavorites())
                + (stat.getCompares() == null ? 0 : stat.getCompares());
        funnel.setEngages(engages);
        funnel.setSelections(stat.getSelectionSearches() == null ? 0 : stat.getSelectionSearches());
        int inquiries = (stat.getInquiries() == null ? 0 : stat.getInquiries())
                + (stat.getProcurements() == null ? 0 : stat.getProcurements());
        funnel.setInquiries(inquiries);
        return funnel;
    }

    // ========== P0-8: 生态Dashboard ==========

    @Override
    public EcosystemDashboardVO ecosystemDashboard(int days) {
        int trendDays = Math.max(7, Math.min(days, 30));
        String cacheKey = Constants.CACHE_GROWTH_PREFIX + "ecosystem:" + trendDays;
        try {
            EcosystemDashboardVO cached = redisUtils.getObj(cacheKey, EcosystemDashboardVO.class);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis生态Dashboard缓存读取失败，降级到DB: error={}", e.getMessage());
        }

        EcosystemDashboardVO vo = new EcosystemDashboardVO();
        LocalDate today = LocalDate.now();

        // 今日生态概览
        GrowthDailyStat todayStat = growthDailyStatMapper.selectOne(Wrappers.<GrowthDailyStat>lambdaQuery()
                .eq(GrowthDailyStat::getStatDate, today));
        vo.setToday(toEcosystemToday(todayStat));

        // 趋势
        List<GrowthDailyStat> stats = growthDailyStatMapper.selectList(Wrappers.<GrowthDailyStat>lambdaQuery()
                .ge(GrowthDailyStat::getStatDate, today.minusDays(trendDays - 1))
                .orderByAsc(GrowthDailyStat::getStatDate));
        List<EcosystemDashboardVO.EcosystemDailyItem> trend = new ArrayList<>();
        List<EcosystemDashboardVO.NotificationCtrItem> ctrTrend = new ArrayList<>();
        for (GrowthDailyStat s : stats) {
            trend.add(toEcosystemDailyItem(s));
            ctrTrend.add(toNotificationCtrItem(s));
        }
        if (trendDays <= 7) {
            vo.setTrend7d(trend);
        } else {
            vo.setTrend30d(trend);
        }
        vo.setNotificationCtrTrend(ctrTrend);

        // 信誉等级分布
        vo.setReputationDistribution(buildReputationDistribution());

        // Trust等级分布
        vo.setTrustDistribution(buildTrustDistribution());

        // Pipeline转化漏斗
        vo.setPipelineFunnel(buildPipelineFunnel());

        // 订阅类型分布
        vo.setSubscriptionTypeDistribution(buildSubscriptionTypeDistribution());

        // 缓存
        try {
            redisUtils.setObj(cacheKey, vo, CACHE_SECONDS + (long) (Math.random() * 30), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis生态Dashboard缓存写入失败（不影响返回）: error={}", e.getMessage());
        }

        return vo;
    }

    private EcosystemDashboardVO.EcosystemToday toEcosystemToday(GrowthDailyStat stat) {
        EcosystemDashboardVO.EcosystemToday today = new EcosystemDashboardVO.EcosystemToday();
        if (stat == null) {
            today.setReturningUsers(0);
            today.setSubscriptionCount(0);
            today.setNotificationSent(0);
            today.setNotificationClicked(0);
            today.setNotificationCtr(0.0);
            today.setContributionUsers(0);
            today.setAcceptedCorrections(0);
            today.setProcurementLeads(0);
            today.setEnterpriseResponses(0);
            today.setFollowConversions(0);
            return today;
        }
        today.setReturningUsers(stat.getReturningUsers() == null ? 0 : stat.getReturningUsers());
        today.setSubscriptionCount(stat.getSubscriptionCount() == null ? 0 : stat.getSubscriptionCount());
        today.setNotificationSent(stat.getNotificationSent() == null ? 0 : stat.getNotificationSent());
        today.setNotificationClicked(stat.getNotificationClicked() == null ? 0 : stat.getNotificationClicked());
        int sent = stat.getNotificationSent() == null ? 0 : stat.getNotificationSent();
        int clicked = stat.getNotificationClicked() == null ? 0 : stat.getNotificationClicked();
        today.setNotificationCtr(sent > 0 ? Math.round(clicked * 10000.0 / sent) / 100.0 : 0.0);
        today.setContributionUsers(stat.getContributionUsers() == null ? 0 : stat.getContributionUsers());
        today.setAcceptedCorrections(stat.getAcceptedCorrections() == null ? 0 : stat.getAcceptedCorrections());
        today.setProcurementLeads(stat.getProcurementLeads() == null ? 0 : stat.getProcurementLeads());
        today.setEnterpriseResponses(stat.getEnterpriseResponses() == null ? 0 : stat.getEnterpriseResponses());
        today.setFollowConversions(stat.getFollowConversions() == null ? 0 : stat.getFollowConversions());
        return today;
    }

    private EcosystemDashboardVO.EcosystemDailyItem toEcosystemDailyItem(GrowthDailyStat s) {
        EcosystemDashboardVO.EcosystemDailyItem item = new EcosystemDashboardVO.EcosystemDailyItem();
        item.setDate(s.getStatDate() == null ? null : s.getStatDate().toString());
        item.setReturningUsers(s.getReturningUsers() == null ? 0 : s.getReturningUsers());
        item.setSubscriptionCount(s.getSubscriptionCount() == null ? 0 : s.getSubscriptionCount());
        item.setNotificationSent(s.getNotificationSent() == null ? 0 : s.getNotificationSent());
        item.setNotificationClicked(s.getNotificationClicked() == null ? 0 : s.getNotificationClicked());
        item.setContributionUsers(s.getContributionUsers() == null ? 0 : s.getContributionUsers());
        item.setProcurementLeads(s.getProcurementLeads() == null ? 0 : s.getProcurementLeads());
        item.setEnterpriseResponses(s.getEnterpriseResponses() == null ? 0 : s.getEnterpriseResponses());
        return item;
    }

    private EcosystemDashboardVO.NotificationCtrItem toNotificationCtrItem(GrowthDailyStat s) {
        EcosystemDashboardVO.NotificationCtrItem item = new EcosystemDashboardVO.NotificationCtrItem();
        item.setDate(s.getStatDate() == null ? null : s.getStatDate().toString());
        int sent = s.getNotificationSent() == null ? 0 : s.getNotificationSent();
        int clicked = s.getNotificationClicked() == null ? 0 : s.getNotificationClicked();
        item.setSent(sent);
        item.setClicked(clicked);
        item.setCtr(sent > 0 ? Math.round(clicked * 10000.0 / sent) / 100.0 : 0.0);
        return item;
    }

    private List<EcosystemDashboardVO.LevelDistributionItem> buildReputationDistribution() {
        List<EcosystemDashboardVO.LevelDistributionItem> result = new ArrayList<>();
        String[][] levels = {
                {Constants.REP_NEW, "新手"},
                {Constants.REP_CONTRIBUTOR, "贡献者"},
                {Constants.REP_ACTIVE, "活跃用户"},
                {Constants.REP_TRUSTED, "可信用户"},
                {Constants.REP_EXPERT, "专家"}
        };
        for (String[] lv : levels) {
            Long count = userReputationMapper.selectCount(
                    Wrappers.<UserReputation>lambdaQuery()
                            .eq(UserReputation::getReputationLevel, lv[0]));
            EcosystemDashboardVO.LevelDistributionItem item = new EcosystemDashboardVO.LevelDistributionItem();
            item.setLevel(lv[0]);
            item.setLabel(lv[1]);
            item.setCount(count.intValue());
            result.add(item);
        }
        return result;
    }

    private List<EcosystemDashboardVO.LevelDistributionItem> buildTrustDistribution() {
        List<EcosystemDashboardVO.LevelDistributionItem> result = new ArrayList<>();
        String[][] levels = {
                {Constants.TRUST_VERIFIED, "已验证"},
                {Constants.TRUST_HIGH, "高可信"},
                {Constants.TRUST_NORMAL, "普通"},
                {Constants.TRUST_LOW, "低可信"}
        };
        for (String[] lv : levels) {
            Long count = robotMapper.selectCount(
                    Wrappers.<Robot>lambdaQuery()
                            .eq(Robot::getTrustLevel, lv[0]));
            EcosystemDashboardVO.LevelDistributionItem item = new EcosystemDashboardVO.LevelDistributionItem();
            item.setLevel(lv[0]);
            item.setLabel(lv[1]);
            item.setCount(count.intValue());
            result.add(item);
        }
        return result;
    }

    private EcosystemDashboardVO.PipelineFunnel buildPipelineFunnel() {
        EcosystemDashboardVO.PipelineFunnel funnel = new EcosystemDashboardVO.PipelineFunnel();
        String[] statuses = {
                Constants.PIPELINE_NEW, Constants.PIPELINE_CONTACTED, Constants.PIPELINE_QUALIFIED,
                Constants.PIPELINE_MATCHING, Constants.PIPELINE_RESPONDED, Constants.PIPELINE_NEGOTIATING,
                Constants.PIPELINE_WON, Constants.PIPELINE_LOST, Constants.PIPELINE_CLOSED
        };
        Integer[] counts = new Integer[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            Long c = inquiryMapper.selectCount(
                    Wrappers.<Inquiry>lambdaQuery()
                            .eq(Inquiry::getPipelineStatus, statuses[i]));
            counts[i] = c.intValue();
        }
        funnel.setNewLeads(counts[0]);
        funnel.setContacted(counts[1]);
        funnel.setQualified(counts[2]);
        funnel.setMatching(counts[3]);
        funnel.setResponded(counts[4]);
        funnel.setNegotiating(counts[5]);
        funnel.setWon(counts[6]);
        funnel.setLost(counts[7]);
        funnel.setClosed(counts[8]);
        return funnel;
    }

    private List<EcosystemDashboardVO.SubscriptionTypeItem> buildSubscriptionTypeDistribution() {
        List<EcosystemDashboardVO.SubscriptionTypeItem> result = new ArrayList<>();
        String[] types = {"ROBOT", "BRAND", "COMPANY"};
        for (String type : types) {
            Long count = userSubscriptionMapper.selectCount(
                    Wrappers.<UserSubscription>lambdaQuery()
                            .eq(UserSubscription::getTargetType, type)
                            .eq(UserSubscription::getEnabled, 1));
            EcosystemDashboardVO.SubscriptionTypeItem item = new EcosystemDashboardVO.SubscriptionTypeItem();
            item.setTargetType(type);
            item.setCount(count.intValue());
            result.add(item);
        }
        return result;
    }
}