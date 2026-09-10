package com.robot.home.home.service.impl;

import com.robot.home.article.service.ArticleService;
import com.robot.home.article.vo.ArticleListVO;
import com.robot.home.banner.entity.Banner;
import com.robot.home.banner.service.BannerService;
import com.robot.home.brand.service.BrandService;
import com.robot.home.brand.vo.BrandListVO;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.community.service.CommunityService;
import com.robot.home.community.vo.PostVO;
import com.robot.home.common.PageResult;
import com.robot.home.company.service.CompanyService;
import com.robot.home.company.vo.CompanyListVO;
import com.robot.home.home.service.HomeService;
import com.robot.home.home.vo.HomeIndexVO;
import com.robot.home.home.vo.RankingBlockVO;
import com.robot.home.ranking.service.RankingService;
import com.robot.home.robot.service.RobotService;
import com.robot.home.robot.vo.CategoryNodeVO;
import com.robot.home.robot.vo.RobotFilterVO;
import com.robot.home.robot.vo.RobotListVO;
import com.robot.home.video.service.VideoService;
import com.robot.home.video.vo.VideoListVO;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 首页聚合服务实现
 */
@Service
public class HomeServiceImpl implements HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeServiceImpl.class);

    @Resource
    private BannerService bannerService;
    @Resource
    private RobotService robotService;
    @Resource
    private BrandService brandService;
    @Resource
    private CompanyService companyService;
    @Resource
    private ArticleService articleService;
    @Resource
    private VideoService videoService;
    @Resource
    private CommunityService communityService;
    @Resource
    private RankingService rankingService;
    @Resource
    private RedisUtils redisUtils;

    /** 首页缓存时间（秒） */
    private static final long HOME_CACHE_SECONDS = 180L;

    @Override
    public HomeIndexVO index(String position) {
        String pos = "app".equals(position) ? "app" : "pc";
        // 尝试从缓存获取（Redis故障时降级到DB查询）
        String cacheKey = "robot:home:" + pos;
        try {
            HomeIndexVO cached = redisUtils.getObj(cacheKey, HomeIndexVO.class);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis首页缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }

        Long userId = SecurityUtils.currentUserId();

        HomeIndexVO vo = new HomeIndexVO();
        List<Banner> banners = bannerService.list(pos);
        vo.setBanners(banners == null ? new ArrayList<>() : banners);

        RobotFilterVO filters = robotService.filters();
        List<CategoryNodeVO> nav = filters.getCategories();
        vo.setQuickNav(nav == null ? new ArrayList<>() : nav);

        vo.setHotRobots(robotService.hot(8, userId));
        vo.setNewRobots(robotService.newest(8, userId));
        vo.setHotBrands(brandService.hot(12));
        vo.setArticles(articleService.page(null, null, 1, 8).getList());
        vo.setVideos(videoService.page(null, null, 1, 6).getList());
        vo.setCompanies(companyService.hot(8));

        PageResult<PostVO> posts = communityService.posts(null, null, null, "hot", userId, 1, 6);
        vo.setHotPosts(posts.getList());

        List<RankingBlockVO> blocks = new ArrayList<>();
        blocks.add(block(Constants.RANK_HOT, "热门机器人榜", 10));
        blocks.add(block(Constants.RANK_HUMANOID, "人形机器人榜", 10));
        blocks.add(block(Constants.RANK_QUADRUPED, "机器狗榜", 10));
        blocks.add(block(Constants.RANK_INDUSTRIAL, "工业机器人榜", 10));
        vo.setRankings(blocks);
        // 缓存3分钟（Redis故障时跳过缓存写入）
        try {
            redisUtils.setObj(cacheKey, vo, HOME_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis首页缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
        return vo;
    }

    private RankingBlockVO block(String code, String name, int limit) {
        RankingBlockVO block = new RankingBlockVO();
        block.setCode(code);
        block.setName(name);
        List<RobotListVO> robots = rankingService.rank(code, limit, SecurityUtils.currentUserId(), "all");
        block.setRobots(robots == null ? new ArrayList<>() : robots);
        return block;
    }
}
