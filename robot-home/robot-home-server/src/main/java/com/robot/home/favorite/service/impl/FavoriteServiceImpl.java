package com.robot.home.favorite.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.service.BizCounter;
import com.robot.home.common.util.PageUtils;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.favorite.entity.Favorite;
import com.robot.home.favorite.mapper.FavoriteMapper;
import com.robot.home.favorite.service.FavoriteService;
import com.robot.home.favorite.vo.FavoriteItemVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.tutorial.entity.Tutorial;
import com.robot.home.tutorial.mapper.TutorialMapper;
import com.robot.home.video.entity.Video;
import com.robot.home.video.mapper.VideoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统一收藏服务实现
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Resource
    private BizCounter bizCounter;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private VideoMapper videoMapper;
    @Resource
    private TutorialMapper tutorialMapper;
    @Resource
    private CommunityPostMapper postMapper;

    @Override
    public boolean check(Long userId, String bizType, Long bizId) {
        if (userId == null) {
            return false;
        }
        return count(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBizType, bizType)
                .eq(Favorite::getBizId, bizId)) > 0;
    }

    @Override
    public Set<Long> checkBatch(Long userId, String bizType, List<Long> bizIds) {
        if (userId == null || bizIds == null || bizIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Favorite> list = list(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBizType, bizType)
                .in(Favorite::getBizId, bizIds));
        return list.stream().map(Favorite::getBizId).collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, String bizType, Long bizId) {
        Favorite exist = getOne(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBizType, bizType)
                .eq(Favorite::getBizId, bizId), false);
        if (exist != null) {
            removeById(exist.getId());
            bizCounter.decr(bizType, bizId, BizCounter.Field.FAVORITE);
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setBizType(bizType);
        favorite.setBizId(bizId);
        save(favorite);
        bizCounter.incr(bizType, bizId, BizCounter.Field.FAVORITE);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, String bizType, Long bizId) {
        int rows = baseMapper.delete(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBizType, bizType)
                .eq(Favorite::getBizId, bizId));
        if (rows > 0) {
            bizCounter.decr(bizType, bizId, BizCounter.Field.FAVORITE);
        }
    }

    @Override
    public PageResult<FavoriteItemVO> myFavorites(Long userId, String bizType, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Favorite> page = new Page<>(pn, ps);
        IPage<Favorite> result = page(page, Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(StrUtil.isNotBlank(bizType), Favorite::getBizType, bizType)
                .orderByDesc(Favorite::getCreateTime));

        List<FavoriteItemVO> items = new ArrayList<>();
        for (Favorite f : result.getRecords()) {
            FavoriteItemVO vo = new FavoriteItemVO();
            vo.setId(f.getId());
            vo.setBizType(f.getBizType());
            vo.setBizId(f.getBizId());
            vo.setCreateTime(f.getCreateTime());
            fillBizInfo(vo, f.getBizType(), f.getBizId());
            items.add(vo);
        }
        return PageResult.of(pn, ps, result.getTotal(), items);
    }

    @Override
    public long count(Long userId, String bizType) {
        return count(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(StrUtil.isNotBlank(bizType), Favorite::getBizType, bizType));
    }

    @Override
    public Map<Long, Long> countByBizIds(String bizType, List<Long> bizIds) {
        Map<Long, Long> map = new HashMap<>();
        if (bizIds == null || bizIds.isEmpty()) {
            return map;
        }
        List<Map<String, Object>> rows = baseMapper.selectMaps(Wrappers.<Favorite>query()
                .select("biz_id", "count(*) as cnt")
                .eq("biz_type", bizType)
                .in("biz_id", bizIds)
                .groupBy("biz_id"));
        for (Map<String, Object> row : rows) {
            Object bizId = row.get("biz_id");
            Object cnt = row.get("cnt");
            if (bizId != null && cnt != null) {
                map.put(Long.valueOf(bizId.toString()), Long.valueOf(cnt.toString()));
            }
        }
        return map;
    }

    /**
     * 按业务类型回填标题、图片与跳转地址
     */
    private void fillBizInfo(FavoriteItemVO vo, String bizType, Long bizId) {
        switch (bizType) {
            case "robot": {
                Robot r = robotMapper.selectById(bizId);
                if (r != null) {
                    vo.setTitle(r.getName());
                    vo.setSubtitle(r.getSubtitle());
                    vo.setImage(r.getCoverImage());
                    vo.setUrl("/robot/" + bizId);
                }
                break;
            }
            case "article": {
                Article a = articleMapper.selectById(bizId);
                if (a != null) {
                    vo.setTitle(a.getTitle());
                    vo.setSubtitle(a.getSummary());
                    vo.setImage(a.getCover());
                    vo.setUrl("/article/" + bizId);
                }
                break;
            }
            case "video": {
                Video v = videoMapper.selectById(bizId);
                if (v != null) {
                    vo.setTitle(v.getTitle());
                    vo.setSubtitle(v.getSummary());
                    vo.setImage(v.getCover());
                    vo.setUrl("/video/" + bizId);
                }
                break;
            }
            case "tutorial": {
                Tutorial t = tutorialMapper.selectById(bizId);
                if (t != null) {
                    vo.setTitle(t.getTitle());
                    vo.setSubtitle(t.getSummary());
                    vo.setImage(t.getCover());
                    vo.setUrl("/tutorial/" + bizId);
                }
                break;
            }
            case "post": {
                CommunityPost p = postMapper.selectById(bizId);
                if (p != null) {
                    vo.setTitle(p.getTitle() != null ? p.getTitle() : excerpt(p.getContent()));
                    vo.setSubtitle(excerpt(p.getContent()));
                    vo.setUrl("/community/" + bizId);
                }
                break;
            }
            default:
                break;
        }
    }

    private String excerpt(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > 60 ? content.substring(0, 60) + "..." : content;
    }
}
