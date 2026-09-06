package com.robot.home.article.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.article.entity.Article;
import com.robot.home.article.entity.ArticleCategory;
import com.robot.home.article.mapper.ArticleCategoryMapper;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.article.service.ArticleService;
import com.robot.home.article.vo.ArticleDetailVO;
import com.robot.home.article.vo.ArticleListVO;
import com.robot.home.article.vo.ArticleNeighborVO;
import com.robot.home.article.vo.CategoryCountVO;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.service.BizCounter;
import com.robot.home.common.util.JsonUtils;
import com.robot.home.common.util.PageUtils;
import com.robot.home.favorite.service.FavoriteService;
import com.robot.home.history.service.HistoryService;
import com.robot.home.like.service.LikeService;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资讯服务实现
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Resource
    private ArticleCategoryMapper categoryMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private FavoriteService favoriteService;
    @Resource
    private LikeService likeService;
    @Resource
    private HistoryService historyService;
    @Resource
    private BizCounter bizCounter;

    @Override
    public PageResult<ArticleListVO> page(Long categoryId, String keyword, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Article> page = new Page<>(pn, ps);
        IPage<Article> result = page(page, Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, 1)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .and(StrUtil.isNotBlank(keyword), w -> w.likeRight(Article::getTitle, keyword).or().like(Article::getSummary, keyword))
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getPublishTime));
        List<ArticleListVO> vos = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        fillCategoryNames(vos);
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO detail(Long id, Long currentUserId) {
        Article article = getById(id);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException("资讯不存在或已下架");
        }
        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setCategoryId(article.getCategoryId());
        vo.setTitle(article.getTitle());
        vo.setCover(article.getCover());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setAuthor(article.getAuthor());
        vo.setSource(article.getSource());
        vo.setTags(JsonUtils.parseStringList(article.getTags()));
        vo.setViewCount(article.getViewCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setFavoriteCount(article.getFavoriteCount());
        vo.setCommentCount(article.getCommentCount());
        vo.setPublishTime(article.getPublishTime());

        if (article.getCategoryId() != null) {
            ArticleCategory cat = categoryMapper.selectById(article.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
            }
        }
        if (article.getBrandId() != null) {
            Brand brand = brandMapper.selectById(article.getBrandId());
            if (brand != null) {
                vo.setBrandId(brand.getId());
                vo.setBrandName(brand.getName());
            }
        }
        if (article.getRobotId() != null) {
            Robot robot = robotMapper.selectById(article.getRobotId());
            if (robot != null) {
                vo.setRobotId(robot.getId());
                vo.setRobotName(robot.getName());
            }
        }
        if (currentUserId != null) {
            vo.setLiked(likeService.check(currentUserId, "article", id));
            vo.setFavorited(favoriteService.check(currentUserId, "article", id));
        } else {
            vo.setLiked(false);
            vo.setFavorited(false);
        }

        // 上下篇（同栏目按发布时间排序）
        Article prev = getOne(Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, 1)
                .eq(article.getCategoryId() != null, Article::getCategoryId, article.getCategoryId())
                .lt(article.getPublishTime() != null, Article::getPublishTime, article.getPublishTime())
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT 1"), false);
        Article next = getOne(Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, 1)
                .eq(article.getCategoryId() != null, Article::getCategoryId, article.getCategoryId())
                .gt(Article::getPublishTime, article.getPublishTime())
                .orderByAsc(Article::getPublishTime)
                .last("LIMIT 1"), false);
        if (prev != null) {
            ArticleNeighborVO n = new ArticleNeighborVO();
            n.setId(prev.getId());
            n.setTitle(prev.getTitle());
            vo.setPrev(n);
        }
        if (next != null) {
            ArticleNeighborVO n = new ArticleNeighborVO();
            n.setId(next.getId());
            n.setTitle(next.getTitle());
            vo.setNext(n);
        }

        bizCounter.incr("article", id, BizCounter.Field.VIEW);
        if (currentUserId != null) {
            historyService.record(currentUserId, "article", id);
        }
        return vo;
    }

    @Override
    public List<ArticleListVO> hot(int limit) {
        List<Article> list = list(Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        List<ArticleListVO> vos = list.stream().map(this::toListVO).collect(Collectors.toList());
        fillCategoryNames(vos);
        return vos;
    }

    @Override
    public List<CategoryCountVO> categories() {
        List<ArticleCategory> cats = categoryMapper.selectList(Wrappers.<ArticleCategory>lambdaQuery()
                .eq(ArticleCategory::getStatus, 1)
                .orderByAsc(ArticleCategory::getSort));
        Map<Long, Long> counts = new HashMap<>();
        List<Map<String, Object>> rows = baseMapper.selectMaps(Wrappers.<Article>query()
                .select("category_id", "count(*) as cnt")
                .eq("status", 1)
                .eq("deleted", 0)
                .groupBy("category_id"));
        for (Map<String, Object> row : rows) {
            Object cid = row.get("category_id");
            Object cnt = row.get("cnt");
            if (cid != null && cnt != null) {
                counts.put(Long.valueOf(cid.toString()), Long.valueOf(cnt.toString()));
            }
        }
        return cats.stream().map(c -> new CategoryCountVO(c.getId(), c.getName(), c.getSort(),
                counts.getOrDefault(c.getId(), 0L))).collect(Collectors.toList());
    }

    private void fillCategoryNames(List<ArticleListVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> ids = vos.stream().map(ArticleListVO::getCategoryId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (ArticleCategory c : categoryMapper.selectBatchIds(ids)) {
            names.put(c.getId(), c.getName());
        }
        for (ArticleListVO vo : vos) {
            if (vo.getCategoryId() != null) {
                vo.setCategoryName(names.get(vo.getCategoryId()));
            }
        }
    }

    private ArticleListVO toListVO(Article a) {
        ArticleListVO vo = new ArticleListVO();
        vo.setId(a.getId());
        vo.setCategoryId(a.getCategoryId());
        vo.setTitle(a.getTitle());
        vo.setCover(a.getCover());
        vo.setSummary(a.getSummary());
        vo.setAuthor(a.getAuthor());
        vo.setSource(a.getSource());
        vo.setTags(JsonUtils.parseStringList(a.getTags()));
        vo.setViewCount(a.getViewCount());
        vo.setLikeCount(a.getLikeCount());
        vo.setFavoriteCount(a.getFavoriteCount());
        vo.setCommentCount(a.getCommentCount());
        vo.setIsTop(a.getIsTop());
        vo.setPublishTime(a.getPublishTime());
        return vo;
    }
}
