package com.robot.home.video.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.robot.home.video.entity.Video;
import com.robot.home.video.entity.VideoCategory;
import com.robot.home.video.mapper.VideoCategoryMapper;
import com.robot.home.video.mapper.VideoMapper;
import com.robot.home.video.service.VideoService;
import com.robot.home.video.vo.VideoDetailVO;
import com.robot.home.video.vo.VideoListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 视频服务实现
 */
@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {

    @Resource
    private VideoCategoryMapper categoryMapper;
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
    public PageResult<VideoListVO> page(Long categoryId, String keyword, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Video> page = new Page<>(pn, ps);
        IPage<Video> result = page(page, Wrappers.<Video>lambdaQuery()
                .eq(Video::getStatus, 1)
                .eq(categoryId != null, Video::getCategoryId, categoryId)
                .and(StrUtil.isNotBlank(keyword), w -> w.likeRight(Video::getTitle, keyword).or().like(Video::getSummary, keyword))
                .orderByDesc(Video::getPublishTime));
        List<VideoListVO> vos = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        fillCategoryNames(vos);
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoDetailVO detail(Long id, Long currentUserId) {
        Video video = getById(id);
        if (video == null || !Integer.valueOf(1).equals(video.getStatus())) {
            throw new BusinessException("视频不存在或已下架");
        }
        VideoDetailVO vo = new VideoDetailVO();
        vo.setId(video.getId());
        vo.setCategoryId(video.getCategoryId());
        vo.setTitle(video.getTitle());
        vo.setCover(video.getCover());
        vo.setUrl(video.getUrl());
        vo.setDuration(video.getDuration());
        vo.setSummary(video.getSummary());
        vo.setAuthor(video.getAuthor());
        vo.setTags(JsonUtils.parseStringList(video.getTags()));
        vo.setViewCount(video.getViewCount());
        vo.setLikeCount(video.getLikeCount());
        vo.setFavoriteCount(video.getFavoriteCount());
        vo.setCommentCount(video.getCommentCount());
        vo.setPublishTime(video.getPublishTime());

        if (video.getCategoryId() != null) {
            VideoCategory cat = categoryMapper.selectById(video.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
            }
        }
        if (video.getBrandId() != null) {
            Brand brand = brandMapper.selectById(video.getBrandId());
            if (brand != null) {
                vo.setBrandId(brand.getId());
                vo.setBrandName(brand.getName());
            }
        }
        if (video.getRobotId() != null) {
            Robot robot = robotMapper.selectById(video.getRobotId());
            if (robot != null) {
                vo.setRobotId(robot.getId());
                vo.setRobotName(robot.getName());
            }
        }
        if (currentUserId != null) {
            vo.setLiked(likeService.check(currentUserId, "video", id));
            vo.setFavorited(favoriteService.check(currentUserId, "video", id));
        } else {
            vo.setLiked(false);
            vo.setFavorited(false);
        }

        List<Video> related = list(Wrappers.<Video>lambdaQuery()
                .eq(Video::getStatus, 1)
                .ne(Video::getId, id)
                .eq(video.getCategoryId() != null, Video::getCategoryId, video.getCategoryId())
                .orderByDesc(Video::getViewCount)
                .last("LIMIT 6"));
        vo.setRelated(related.stream().map(this::toListVO).collect(Collectors.toList()));

        bizCounter.incr("video", id, BizCounter.Field.VIEW);
        if (currentUserId != null) {
            historyService.record(currentUserId, "video", id);
        }
        return vo;
    }

    @Override
    public List<VideoListVO> hot(int limit) {
        List<Video> list = list(Wrappers.<Video>lambdaQuery()
                .eq(Video::getStatus, 1)
                .orderByDesc(Video::getViewCount)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        List<VideoListVO> vos = list.stream().map(this::toListVO).collect(Collectors.toList());
        fillCategoryNames(vos);
        return vos;
    }

    @Override
    public List<CategoryCountVO> categories() {
        List<VideoCategory> cats = categoryMapper.selectList(Wrappers.<VideoCategory>lambdaQuery()
                .eq(VideoCategory::getStatus, 1)
                .orderByAsc(VideoCategory::getSort));
        Map<Long, Long> counts = new HashMap<>();
        List<Map<String, Object>> rows = baseMapper.selectMaps(Wrappers.<Video>query()
                .select("category_id", "count(*) as cnt")
                .eq("status", 1)
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

    private void fillCategoryNames(List<VideoListVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> ids = vos.stream().map(VideoListVO::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (VideoCategory c : categoryMapper.selectBatchIds(ids)) {
            names.put(c.getId(), c.getName());
        }
        for (VideoListVO vo : vos) {
            if (vo.getCategoryId() != null) {
                vo.setCategoryName(names.get(vo.getCategoryId()));
            }
        }
    }

    private VideoListVO toListVO(Video v) {
        VideoListVO vo = new VideoListVO();
        vo.setId(v.getId());
        vo.setCategoryId(v.getCategoryId());
        vo.setTitle(v.getTitle());
        vo.setCover(v.getCover());
        vo.setUrl(v.getUrl());
        vo.setDuration(v.getDuration());
        vo.setSummary(v.getSummary());
        vo.setAuthor(v.getAuthor());
        vo.setTags(JsonUtils.parseStringList(v.getTags()));
        vo.setViewCount(v.getViewCount());
        vo.setLikeCount(v.getLikeCount());
        vo.setFavoriteCount(v.getFavoriteCount());
        vo.setCommentCount(v.getCommentCount());
        vo.setPublishTime(v.getPublishTime());
        return vo;
    }
}
