package com.robot.home.history.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.history.entity.BrowseHistory;
import com.robot.home.history.mapper.BrowseHistoryMapper;
import com.robot.home.history.service.HistoryService;
import com.robot.home.history.vo.HistoryItemVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.tutorial.entity.Tutorial;
import com.robot.home.tutorial.mapper.TutorialMapper;
import com.robot.home.video.entity.Video;
import com.robot.home.video.mapper.VideoMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 浏览历史服务实现
 */
@Service
public class HistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> implements HistoryService {

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
    public void record(Long userId, String bizType, Long bizId) {
        if (userId == null || bizId == null) {
            return;
        }
        // 同一业务对象只保留最近一条记录
        baseMapper.delete(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId)
                .eq(BrowseHistory::getBizType, bizType)
                .eq(BrowseHistory::getBizId, bizId));
        BrowseHistory history = new BrowseHistory();
        history.setUserId(userId);
        history.setBizType(bizType);
        history.setBizId(bizId);
        save(history);
    }

    @Override
    public PageResult<HistoryItemVO> myHistory(Long userId, String bizType, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<BrowseHistory> page = new Page<>(pn, ps);
        IPage<BrowseHistory> result = page(page, Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId)
                .eq(StrUtil.isNotBlank(bizType), BrowseHistory::getBizType, bizType)
                .orderByDesc(BrowseHistory::getCreateTime));

        List<HistoryItemVO> items = new ArrayList<>();
        for (BrowseHistory h : result.getRecords()) {
            HistoryItemVO vo = new HistoryItemVO();
            vo.setId(h.getId());
            vo.setBizType(h.getBizType());
            vo.setBizId(h.getBizId());
            vo.setCreateTime(h.getCreateTime());
            fillBizInfo(vo, h.getBizType(), h.getBizId());
            items.add(vo);
        }
        return PageResult.of(pn, ps, result.getTotal(), items);
    }

    @Override
    public void remove(Long userId, String bizType, Long bizId) {
        baseMapper.delete(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId)
                .eq(BrowseHistory::getBizType, bizType)
                .eq(BrowseHistory::getBizId, bizId));
    }

    @Override
    public void clear(Long userId, String bizType) {
        baseMapper.delete(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId)
                .eq(StrUtil.isNotBlank(bizType), BrowseHistory::getBizType, bizType));
    }

    @Override
    public long count(Long userId) {
        return count(Wrappers.<BrowseHistory>lambdaQuery().eq(BrowseHistory::getUserId, userId));
    }

    private void fillBizInfo(HistoryItemVO vo, String bizType, Long bizId) {
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
                    vo.setTitle(StrUtil.isNotBlank(p.getTitle()) ? p.getTitle() : excerpt(p.getContent()));
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
