package com.robot.home.tutorial.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.article.vo.CategoryCountVO;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.service.BizCounter;
import com.robot.home.common.util.JsonUtils;
import com.robot.home.common.util.PageUtils;
import com.robot.home.favorite.service.FavoriteService;
import com.robot.home.history.service.HistoryService;
import com.robot.home.like.service.LikeService;
import com.robot.home.tutorial.entity.Tutorial;
import com.robot.home.tutorial.entity.TutorialCategory;
import com.robot.home.tutorial.mapper.TutorialCategoryMapper;
import com.robot.home.tutorial.mapper.TutorialMapper;
import com.robot.home.tutorial.service.TutorialService;
import com.robot.home.tutorial.vo.TutorialDetailVO;
import com.robot.home.tutorial.vo.TutorialListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 教程服务实现
 */
@Service
public class TutorialServiceImpl extends ServiceImpl<TutorialMapper, Tutorial> implements TutorialService {

    @Resource
    private TutorialCategoryMapper categoryMapper;
    @Resource
    private FavoriteService favoriteService;
    @Resource
    private LikeService likeService;
    @Resource
    private HistoryService historyService;
    @Resource
    private BizCounter bizCounter;

    @Override
    public PageResult<TutorialListVO> page(Long categoryId, String keyword, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Tutorial> page = new Page<>(pn, ps);
        IPage<Tutorial> result = page(page, Wrappers.<Tutorial>lambdaQuery()
                .eq(Tutorial::getStatus, 1)
                .eq(categoryId != null, Tutorial::getCategoryId, categoryId)
                .and(StrUtil.isNotBlank(keyword), w -> w.likeRight(Tutorial::getTitle, keyword).or().like(Tutorial::getSummary, keyword))
                .orderByDesc(Tutorial::getPublishTime));
        List<TutorialListVO> vos = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        fillCategoryNames(vos);
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TutorialDetailVO detail(Long id, Long currentUserId) {
        Tutorial tutorial = getById(id);
        if (tutorial == null || !Integer.valueOf(1).equals(tutorial.getStatus())) {
            throw new BusinessException("教程不存在或已下架");
        }
        TutorialDetailVO vo = new TutorialDetailVO();
        vo.setId(tutorial.getId());
        vo.setCategoryId(tutorial.getCategoryId());
        vo.setTitle(tutorial.getTitle());
        vo.setCover(tutorial.getCover());
        vo.setSummary(tutorial.getSummary());
        vo.setContent(tutorial.getContent());
        vo.setAuthor(tutorial.getAuthor());
        vo.setTags(JsonUtils.parseStringList(tutorial.getTags()));
        vo.setViewCount(tutorial.getViewCount());
        vo.setLikeCount(tutorial.getLikeCount());
        vo.setFavoriteCount(tutorial.getFavoriteCount());
        vo.setCommentCount(tutorial.getCommentCount());
        vo.setPublishTime(tutorial.getPublishTime());
        if (tutorial.getCategoryId() != null) {
            TutorialCategory cat = categoryMapper.selectById(tutorial.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
            }
        }
        if (currentUserId != null) {
            vo.setLiked(likeService.check(currentUserId, "tutorial", id));
            vo.setFavorited(favoriteService.check(currentUserId, "tutorial", id));
        } else {
            vo.setLiked(false);
            vo.setFavorited(false);
        }
        List<Tutorial> related = list(Wrappers.<Tutorial>lambdaQuery()
                .eq(Tutorial::getStatus, 1)
                .ne(Tutorial::getId, id)
                .eq(tutorial.getCategoryId() != null, Tutorial::getCategoryId, tutorial.getCategoryId())
                .orderByDesc(Tutorial::getViewCount)
                .last("LIMIT 6"));
        vo.setRelated(related.stream().map(this::toListVO).collect(Collectors.toList()));

        bizCounter.incr("tutorial", id, BizCounter.Field.VIEW);
        if (currentUserId != null) {
            historyService.record(currentUserId, "tutorial", id);
        }
        return vo;
    }

    @Override
    public List<TutorialListVO> hot(int limit) {
        List<Tutorial> list = list(Wrappers.<Tutorial>lambdaQuery()
                .eq(Tutorial::getStatus, 1)
                .orderByDesc(Tutorial::getViewCount)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        List<TutorialListVO> vos = list.stream().map(this::toListVO).collect(Collectors.toList());
        fillCategoryNames(vos);
        return vos;
    }

    @Override
    public List<CategoryCountVO> categories() {
        List<TutorialCategory> cats = categoryMapper.selectList(Wrappers.<TutorialCategory>lambdaQuery()
                .eq(TutorialCategory::getStatus, 1)
                .orderByAsc(TutorialCategory::getSort));
        Map<Long, Long> counts = new HashMap<>();
        List<Map<String, Object>> rows = baseMapper.selectMaps(Wrappers.<Tutorial>query()
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

    private void fillCategoryNames(List<TutorialListVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> ids = vos.stream().map(TutorialListVO::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (TutorialCategory c : categoryMapper.selectBatchIds(ids)) {
            names.put(c.getId(), c.getName());
        }
        for (TutorialListVO vo : vos) {
            if (vo.getCategoryId() != null) {
                vo.setCategoryName(names.get(vo.getCategoryId()));
            }
        }
    }

    private TutorialListVO toListVO(Tutorial t) {
        TutorialListVO vo = new TutorialListVO();
        vo.setId(t.getId());
        vo.setCategoryId(t.getCategoryId());
        vo.setTitle(t.getTitle());
        vo.setCover(t.getCover());
        vo.setSummary(t.getSummary());
        vo.setAuthor(t.getAuthor());
        vo.setTags(JsonUtils.parseStringList(t.getTags()));
        vo.setViewCount(t.getViewCount());
        vo.setLikeCount(t.getLikeCount());
        vo.setFavoriteCount(t.getFavoriteCount());
        vo.setCommentCount(t.getCommentCount());
        vo.setPublishTime(t.getPublishTime());
        return vo;
    }
}
