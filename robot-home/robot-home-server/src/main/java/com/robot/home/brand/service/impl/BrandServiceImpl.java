package com.robot.home.brand.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.brand.service.BrandService;
import com.robot.home.brand.vo.BrandDetailVO;
import com.robot.home.brand.vo.BrandLetterGroupVO;
import com.robot.home.brand.vo.BrandListVO;
import com.robot.home.brand.vo.BrandPageVO;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.follow.service.FollowService;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.vo.RobotSummaryVO;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.qa.entity.RobotQuestion;
import com.robot.home.qa.mapper.RobotQuestionMapper;
import com.robot.home.review.entity.RobotReview;
import com.robot.home.review.mapper.RobotReviewMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 品牌服务实现
 */
@Service
public class BrandServiceImpl extends ServiceImpl<BrandMapper, Brand> implements BrandService {

    private static final Logger log = LoggerFactory.getLogger(BrandServiceImpl.class);

    private static final long CACHE_SECONDS = 600L;

    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private FollowService followService;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private CommunityPostMapper communityPostMapper;
    @Resource
    private RobotQuestionMapper robotQuestionMapper;
    @Resource
    private RobotReviewMapper robotReviewMapper;

    @Override
    public PageResult<BrandListVO> page(String keyword, String initial, Boolean hot, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Brand> page = new Page<>(pn, ps);
        IPage<Brand> result = page(page, Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .eq(StrUtil.isNotBlank(initial), Brand::getInitial, initial)
                .likeRight(StrUtil.isNotBlank(keyword), Brand::getName, keyword)
                .orderBy(Boolean.TRUE.equals(hot), false, Brand::getHotScore)
                .orderBy(true, true, Brand::getSort)
                .orderByDesc(Brand::getHotScore));
        List<BrandListVO> vos = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public BrandDetailVO detail(Long id) {
        Brand brand = getById(id);
        if (brand == null) {
            throw new BusinessException("品牌不存在");
        }
        BrandDetailVO vo = new BrandDetailVO();
        vo.setBrand(brand);
        if (brand.getCompanyId() != null) {
            Company company = companyMapper.selectById(brand.getCompanyId());
            if (company != null) {
                vo.setCompanyName(company.getName());
            }
        }
        PageResult<RobotSummaryVO> products = robots(id, 1, 12);
        vo.setProductCount(products.getTotal());
        vo.setProducts(products.getList());
        return vo;
    }

    @Override
    public PageResult<RobotSummaryVO> robots(Long brandId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Robot> page = new Page<>(pn, ps);
        IPage<Robot> result = robotMapper.selectPage(page, Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getBrandId, brandId)
                .orderByDesc(Robot::getHotScore));
        List<RobotSummaryVO> vos = result.getRecords().stream().map(r -> {
            RobotSummaryVO vo = new RobotSummaryVO();
            vo.setId(r.getId());
            vo.setName(r.getName());
            vo.setModel(r.getModel());
            vo.setCoverImage(r.getCoverImage());
            vo.setGuidePrice(r.getGuidePrice());
            vo.setStatus(r.getStatus());
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public List<BrandLetterGroupVO> groupByLetter() {
        String key = Constants.CACHE_BRAND_PREFIX + "letter";
        try {
            String cached = redisUtils.get(key);
            if (cached != null) {
                try {
                    List<BrandLetterGroupVO> list = JSONUtil.toList(JSONUtil.parseArray(cached), BrandLetterGroupVO.class);
                    if (list != null) {
                        return list;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败时回源数据库
                }
            }
        } catch (Exception e) {
            log.warn("Redis品牌字母分组缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }
        List<Brand> brands = list(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByAsc(Brand::getInitial)
                .orderByDesc(Brand::getHotScore));
        Map<String, List<BrandListVO>> map = new LinkedHashMap<>();
        for (Brand b : brands) {
            String letter = StrUtil.isBlank(b.getInitial()) ? "#" : b.getInitial().toUpperCase();
            map.computeIfAbsent(letter, k -> new ArrayList<>()).add(toListVO(b));
        }
        List<BrandLetterGroupVO> result = map.entrySet().stream().map(e -> {
            BrandLetterGroupVO g = new BrandLetterGroupVO();
            g.setLetter(e.getKey());
            g.setBrands(e.getValue());
            return g;
        }).collect(Collectors.toList());
        try {
            redisUtils.set(key, JSONUtil.toJsonStr(result), CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis品牌字母分组缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
        return result;
    }

    @Override
    public List<BrandListVO> hot(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        String key = Constants.CACHE_BRAND_PREFIX + "hot:" + size;
        try {
            String cached = redisUtils.get(key);
            if (cached != null) {
                try {
                    List<BrandListVO> list = JSONUtil.toList(JSONUtil.parseArray(cached), BrandListVO.class);
                    if (list != null) {
                        return list;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败时回源数据库
                }
            }
        } catch (Exception e) {
            log.warn("Redis热门品牌缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }
        List<Brand> brands = list(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByDesc(Brand::getHotScore)
                .last("LIMIT " + size));
        List<BrandListVO> result = brands.stream().map(this::toListVO).collect(Collectors.toList());
        try {
            redisUtils.set(key, JSONUtil.toJsonStr(result), CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis热门品牌缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
        return result;
    }

    private BrandListVO toListVO(Brand b) {
        BrandListVO vo = new BrandListVO();
        vo.setId(b.getId());
        vo.setName(b.getName());
        vo.setLogo(b.getLogo());
        vo.setInitial(b.getInitial());
        vo.setIntro(b.getIntro());
        vo.setCountry(b.getCountry());
        vo.setFoundYear(b.getFoundYear());
        vo.setWebsite(b.getWebsite());
        vo.setCompanyId(b.getCompanyId());
        vo.setRobotCount(b.getRobotCount());
        vo.setHotScore(b.getHotScore());
        vo.setFollowCount(b.getFollowCount());
        vo.setArticleCount(b.getArticleCount());
        vo.setReviewCount(b.getReviewCount());
        if (b.getCompanyId() != null) {
            Company c = companyMapper.selectById(b.getCompanyId());
            if (c != null) {
                vo.setCompanyName(c.getName());
            }
        }
        return vo;
    }

    @Override
    public BrandPageVO brandPage(Long brandId, Long currentUserId) {
        Brand brand = getById(brandId);
        if (brand == null || !Integer.valueOf(1).equals(brand.getStatus())) {
            throw new BusinessException("品牌不存在");
        }
        BrandPageVO page = new BrandPageVO();

        // 品牌详情
        BrandDetailVO detailVO = detail(brandId);
        page.setBrand(detailVO);

        // 关注数和关注状态
        page.setFollowCount(brand.getFollowCount() == null ? 0 : brand.getFollowCount());
        if (currentUserId != null) {
            try {
                page.setFollowed(followService.checkBatch(currentUserId, Constants.BIZ_TYPE_BRAND, java.util.Collections.singletonList(brandId)).contains(brandId));
            } catch (Exception e) {
                page.setFollowed(false);
            }
        } else {
            page.setFollowed(false);
        }

        // 热门机器人（按hotScore）
        List<Robot> hotRobotList = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getBrandId, brandId)
                .eq(Robot::getStatus, 1)
                .orderByDesc(Robot::getHotScore)
                .last("LIMIT 6"));
        List<BrandPageVO.RobotSimpleVO> hotRobots = new ArrayList<>();
        for (Robot r : hotRobotList) {
            BrandPageVO.RobotSimpleVO vo = new BrandPageVO.RobotSimpleVO();
            vo.setId(r.getId());
            vo.setName(r.getName());
            vo.setCoverImage(r.getCoverImage());
            vo.setSubtitle(r.getSubtitle());
            vo.setGuidePrice(r.getGuidePrice());
            vo.setScore(r.getScore());
            vo.setFavoriteCount(r.getFavoriteCount());
            hotRobots.add(vo);
        }
        page.setHotRobots(hotRobots);

        // 新品机器人（按releaseDate）
        List<Robot> newRobotList = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getBrandId, brandId)
                .eq(Robot::getStatus, 1)
                .isNotNull(Robot::getReleaseDate)
                .orderByDesc(Robot::getReleaseDate)
                .last("LIMIT 6"));
        List<BrandPageVO.RobotSimpleVO> newRobots = new ArrayList<>();
        for (Robot r : newRobotList) {
            BrandPageVO.RobotSimpleVO vo = new BrandPageVO.RobotSimpleVO();
            vo.setId(r.getId());
            vo.setName(r.getName());
            vo.setCoverImage(r.getCoverImage());
            vo.setSubtitle(r.getSubtitle());
            vo.setGuidePrice(r.getGuidePrice());
            vo.setScore(r.getScore());
            vo.setFavoriteCount(r.getFavoriteCount());
            newRobots.add(vo);
        }
        page.setNewRobots(newRobots);

        // 机器人总数
        Long robotCount = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getBrandId, brandId)
                .eq(Robot::getStatus, 1));
        page.setRobotCount(robotCount);

        // 相关文章
        List<Article> articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .eq(Article::getBrandId, brandId)
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT 5"));
        List<BrandPageVO.ArticleSimpleVO> articleVOs = new ArrayList<>();
        for (Article a : articles) {
            BrandPageVO.ArticleSimpleVO vo = new BrandPageVO.ArticleSimpleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setCoverImage(a.getCover());
            vo.setViewCount(a.getViewCount());
            vo.setPublishTime(a.getPublishTime());
            articleVOs.add(vo);
        }
        page.setArticles(articleVOs);

        // 文章总数
        Long articleCount = articleMapper.selectCount(Wrappers.<Article>lambdaQuery()
                .eq(Article::getBrandId, brandId)
                .eq(Article::getStatus, 1));
        page.setArticleCount(articleCount);

        // 相关评测（通过品牌下的机器人）
        List<Long> robotIds = new ArrayList<>();
        for (Robot r : hotRobotList) {
            robotIds.add(r.getId());
        }
        for (Robot r : newRobotList) {
            if (!robotIds.contains(r.getId())) {
                robotIds.add(r.getId());
            }
        }
        if (!robotIds.isEmpty()) {
            // 补充更多robotIds
            List<Robot> allBrandRobots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                    .eq(Robot::getBrandId, brandId)
                    .eq(Robot::getStatus, 1)
                    .select(Robot::getId));
            for (Robot r : allBrandRobots) {
                if (!robotIds.contains(r.getId())) {
                    robotIds.add(r.getId());
                }
            }
        }

        List<BrandPageVO.ReviewSimpleVO> reviewVOs = new ArrayList<>();
        List<BrandPageVO.PostSimpleVO> postVOs = new ArrayList<>();
        List<BrandPageVO.QuestionSimpleVO> questionVOs = new ArrayList<>();
        Long reviewCount = 0L;

        if (!robotIds.isEmpty()) {
            // 评测
            List<RobotReview> reviews = robotReviewMapper.selectList(Wrappers.<RobotReview>lambdaQuery()
                    .in(RobotReview::getRobotId, robotIds)
                    .eq(RobotReview::getStatus, 1)
                    .orderByDesc(RobotReview::getCreateTime)
                    .last("LIMIT 5"));
            for (RobotReview r : reviews) {
                BrandPageVO.ReviewSimpleVO vo = new BrandPageVO.ReviewSimpleVO();
                vo.setId(r.getId());
                vo.setTitle(null); // Review没有title字段
                vo.setScore(r.getOverallScore() == null ? null : new java.math.BigDecimal(r.getOverallScore()));
                vo.setViewCount(r.getHelpfulCount());
                vo.setCreateTime(r.getCreateTime());
                reviewVOs.add(vo);
            }
            reviewCount = robotReviewMapper.selectCount(Wrappers.<RobotReview>lambdaQuery()
                    .in(RobotReview::getRobotId, robotIds)
                    .eq(RobotReview::getStatus, 1));

            // 社区讨论
            List<CommunityPost> posts = communityPostMapper.selectList(Wrappers.<CommunityPost>lambdaQuery()
                    .in(CommunityPost::getRobotId, robotIds)
                    .eq(CommunityPost::getStatus, 1)
                    .orderByDesc(CommunityPost::getCreateTime)
                    .last("LIMIT 5"));
            for (CommunityPost p : posts) {
                BrandPageVO.PostSimpleVO vo = new BrandPageVO.PostSimpleVO();
                vo.setId(p.getId());
                vo.setTitle(p.getTitle());
                vo.setLikeCount(p.getLikeCount());
                vo.setCommentCount(p.getCommentCount());
                vo.setCreateTime(p.getCreateTime());
                postVOs.add(vo);
            }

            // 问答
            List<RobotQuestion> questions = robotQuestionMapper.selectList(Wrappers.<RobotQuestion>lambdaQuery()
                    .in(RobotQuestion::getRobotId, robotIds)
                    .eq(RobotQuestion::getStatus, 1)
                    .orderByDesc(RobotQuestion::getCreateTime)
                    .last("LIMIT 5"));
            for (RobotQuestion q : questions) {
                BrandPageVO.QuestionSimpleVO vo = new BrandPageVO.QuestionSimpleVO();
                vo.setId(q.getId());
                vo.setTitle(q.getTitle());
                vo.setAnswerCount(q.getAnswerCount());
                vo.setViewCount(q.getViewCount());
                vo.setCreateTime(q.getCreateTime());
                questionVOs.add(vo);
            }
        }

        page.setReviews(reviewVOs);
        page.setPosts(postVOs);
        page.setQuestions(questionVOs);
        page.setReviewCount(reviewCount);

        return page;
    }
}
