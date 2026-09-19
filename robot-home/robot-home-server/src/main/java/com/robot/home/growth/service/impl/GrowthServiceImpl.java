package com.robot.home.growth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.growth.entity.GrowthDailyStat;
import com.robot.home.growth.mapper.GrowthDailyStatMapper;
import com.robot.home.growth.service.GrowthService;
import com.robot.home.growth.vo.GrowthDashboardVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
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
}