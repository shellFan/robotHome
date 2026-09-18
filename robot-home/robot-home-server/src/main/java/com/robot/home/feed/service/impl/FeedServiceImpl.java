package com.robot.home.feed.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.feed.service.FeedService;
import com.robot.home.feed.vo.FeedItemVO;
import com.robot.home.follow.service.FollowService;
import com.robot.home.follow.vo.FollowItemVO;
import com.robot.home.qa.entity.RobotQuestion;
import com.robot.home.qa.mapper.RobotQuestionMapper;
import com.robot.home.review.entity.RobotReview;
import com.robot.home.review.mapper.RobotReviewMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Phase9: 关注动态Feed实现
 * <p>
 * 查询式：根据用户关注列表，查询关注对象的新内容
 * Redis Cache：缓存用户关注列表和Feed结果
 * 只展示PUBLISHED内容
 */
@Service
public class FeedServiceImpl implements FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedServiceImpl.class);

    private static final long FEED_CACHE_SECONDS = 120L;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Resource
    private FollowService followService;
    @Resource
    private CommunityPostMapper communityPostMapper;
    @Resource
    private RobotQuestionMapper robotQuestionMapper;
    @Resource
    private RobotReviewMapper robotReviewMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public PageResult<FeedItemVO> myFeed(Long userId, Long lastId, Integer pageSize) {
        if (userId == null) {
            return PageResult.of(1, DEFAULT_PAGE_SIZE, 0, new ArrayList<>());
        }
        int ps = PageUtils.normalizePageSize(pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);

        // 1. 获取用户关注的对象列表
        List<FollowItemVO> follows = followService.myFollows(userId, null, 1, 200).getList();
        if (follows == null || follows.isEmpty()) {
            return PageResult.of(1, ps, 0, new ArrayList<>());
        }

        // 按类型分组
        List<Long> followedRobotIds = new ArrayList<>();
        List<Long> followedBrandIds = new ArrayList<>();
        List<Long> followedCompanyIds = new ArrayList<>();
        List<Long> followedUserIds = new ArrayList<>();
        for (FollowItemVO f : follows) {
            if (StrUtil.isBlank(f.getFollowType()) || f.getFollowId() == null) {
                continue;
            }
            switch (f.getFollowType()) {
                case Constants.BIZ_TYPE_ROBOT:
                    followedRobotIds.add(f.getFollowId());
                    break;
                case Constants.BIZ_TYPE_BRAND:
                    followedBrandIds.add(f.getFollowId());
                    break;
                case Constants.BIZ_TYPE_COMPANY:
                    followedCompanyIds.add(f.getFollowId());
                    break;
                case Constants.BIZ_TYPE_USER:
                    followedUserIds.add(f.getFollowId());
                    break;
            }
        }

        // 2. 查询各类型的新内容（只查PUBLISHED）
        List<FeedItemVO> feedItems = new ArrayList<>();

        // 关注机器人的新帖子
        if (!followedRobotIds.isEmpty()) {
            addRobotPosts(followedRobotIds, lastId, ps, feedItems);
        }

        // 关注品牌的新文章
        if (!followedBrandIds.isEmpty()) {
            addBrandArticles(followedBrandIds, lastId, ps, feedItems);
        }

        // 关注用户的新帖子
        if (!followedUserIds.isEmpty()) {
            addUserPosts(followedUserIds, lastId, ps, feedItems);
        }

        // 关注机器人的新问题
        if (!followedRobotIds.isEmpty()) {
            addRobotQuestions(followedRobotIds, lastId, ps, feedItems);
        }

        // 3. 按createTime降序排序，取前pageSize条
        feedItems.sort((a, b) -> {
            if (b.getCreateTime() == null || a.getCreateTime() == null) {
                return 0;
            }
            int cmp = b.getCreateTime().compareTo(a.getCreateTime());
            if (cmp != 0) {
                return cmp;
            }
            // 稳定排序：相同时间按ID降序
            return Long.compare(b.getId() == null ? 0 : b.getId(), a.getId() == null ? 0 : a.getId());
        });

        List<FeedItemVO> result = new ArrayList<>();
        for (int i = 0; i < feedItems.size() && i < ps; i++) {
            result.add(feedItems.get(i));
        }

        return PageResult.of(1, ps, (long) feedItems.size(), result);
    }

    private void addRobotPosts(List<Long> robotIds, Long lastId, int pageSize, List<FeedItemVO> feedItems) {
        // 批量查询关注机器人的帖子
        List<CommunityPost> posts = communityPostMapper.selectList(Wrappers.<CommunityPost>lambdaQuery()
                .in(CommunityPost::getRobotId, robotIds)
                .eq(CommunityPost::getStatus, 1)
                .gt(lastId != null, CommunityPost::getId, lastId)
                .orderByDesc(CommunityPost::getCreateTime)
                .last("LIMIT " + pageSize));
        // 填充机器人名
        Map<Long, Robot> robotMap = loadRobotMap(robotIds);
        for (CommunityPost p : posts) {
            FeedItemVO item = new FeedItemVO();
            item.setId(p.getId());
            item.setFeedType("POST");
            item.setTargetId(p.getId());
            item.setTitle(p.getTitle());
            item.setSummary(p.getContent() != null && p.getContent().length() > 100 ? p.getContent().substring(0, 100) : p.getContent());
            item.setSourceType(Constants.BIZ_TYPE_ROBOT);
            item.setSourceId(p.getRobotId());
            Robot r = robotMap.get(p.getRobotId());
            item.setSourceName(r != null ? r.getName() : null);
            item.setInteractionCount(p.getCommentCount());
            item.setCreateTime(p.getCreateTime());
            feedItems.add(item);
        }
    }

    private void addBrandArticles(List<Long> brandIds, Long lastId, int pageSize, List<FeedItemVO> feedItems) {
        List<Article> articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .in(Article::getBrandId, brandIds)
                .eq(Article::getStatus, 1)
                .gt(lastId != null, Article::getId, lastId)
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + pageSize));
        for (Article a : articles) {
            FeedItemVO item = new FeedItemVO();
            item.setId(a.getId());
            item.setFeedType("ARTICLE");
            item.setTargetId(a.getId());
            item.setTitle(a.getTitle());
            item.setSummary(a.getSummary());
            item.setCoverImage(a.getCover());
            item.setSourceType(Constants.BIZ_TYPE_BRAND);
            item.setSourceId(a.getBrandId());
            item.setInteractionCount(a.getViewCount());
            item.setCreateTime(a.getPublishTime() != null ? a.getPublishTime() : a.getCreateTime());
            feedItems.add(item);
        }
    }

    private void addUserPosts(List<Long> userIds, Long lastId, int pageSize, List<FeedItemVO> feedItems) {
        List<CommunityPost> posts = communityPostMapper.selectList(Wrappers.<CommunityPost>lambdaQuery()
                .in(CommunityPost::getUserId, userIds)
                .eq(CommunityPost::getStatus, 1)
                .gt(lastId != null, CommunityPost::getId, lastId)
                .orderByDesc(CommunityPost::getCreateTime)
                .last("LIMIT " + pageSize));
        // 填充用户信息
        Map<Long, User> userMap = loadUserMap(userIds);
        for (CommunityPost p : posts) {
            FeedItemVO item = new FeedItemVO();
            item.setId(p.getId());
            item.setFeedType("POST");
            item.setTargetId(p.getId());
            item.setTitle(p.getTitle());
            item.setSummary(p.getContent() != null && p.getContent().length() > 100 ? p.getContent().substring(0, 100) : p.getContent());
            item.setSourceType(Constants.BIZ_TYPE_USER);
            item.setSourceId(p.getUserId());
            User u = userMap.get(p.getUserId());
            if (u != null) {
                item.setSourceName(u.getNickname());
                item.setAuthorNickname(u.getNickname());
                item.setAuthorAvatar(u.getAvatar());
            }
            item.setInteractionCount(p.getCommentCount());
            item.setCreateTime(p.getCreateTime());
            feedItems.add(item);
        }
    }

    private void addRobotQuestions(List<Long> robotIds, Long lastId, int pageSize, List<FeedItemVO> feedItems) {
        List<RobotQuestion> questions = robotQuestionMapper.selectList(Wrappers.<RobotQuestion>lambdaQuery()
                .in(RobotQuestion::getRobotId, robotIds)
                .eq(RobotQuestion::getStatus, 1)
                .gt(lastId != null, RobotQuestion::getId, lastId)
                .orderByDesc(RobotQuestion::getCreateTime)
                .last("LIMIT " + pageSize));
        Map<Long, Robot> robotMap = loadRobotMap(robotIds);
        for (RobotQuestion q : questions) {
            FeedItemVO item = new FeedItemVO();
            item.setId(q.getId());
            item.setFeedType("QUESTION");
            item.setTargetId(q.getId());
            item.setTitle(q.getTitle());
            item.setSourceType(Constants.BIZ_TYPE_ROBOT);
            item.setSourceId(q.getRobotId());
            Robot r = robotMap.get(q.getRobotId());
            item.setSourceName(r != null ? r.getName() : null);
            item.setInteractionCount(q.getAnswerCount());
            item.setCreateTime(q.getCreateTime());
            feedItems.add(item);
        }
    }

    private Map<Long, Robot> loadRobotMap(List<Long> robotIds) {
        Map<Long, Robot> map = new HashMap<>();
        if (robotIds == null || robotIds.isEmpty()) {
            return map;
        }
        List<Robot> robots = robotMapper.selectBatchIds(robotIds);
        for (Robot r : robots) {
            map.put(r.getId(), r);
        }
        return map;
    }

    private Map<Long, User> loadUserMap(List<Long> userIds) {
        Map<Long, User> map = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        for (User u : users) {
            map.put(u.getId(), u);
        }
        return map;
    }
}