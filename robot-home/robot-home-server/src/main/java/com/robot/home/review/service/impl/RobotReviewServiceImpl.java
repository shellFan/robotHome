package com.robot.home.review.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.PageResult;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.review.dto.ReviewDTO;
import com.robot.home.review.entity.RobotReview;
import com.robot.home.review.entity.RobotReviewHelpful;
import com.robot.home.review.entity.RobotReviewSummary;
import com.robot.home.review.mapper.RobotReviewHelpfulMapper;
import com.robot.home.review.mapper.RobotReviewMapper;
import com.robot.home.review.mapper.RobotReviewSummaryMapper;
import com.robot.home.review.service.RobotReviewService;
import com.robot.home.review.vo.ReviewSummaryVO;
import com.robot.home.review.vo.ReviewUserVO;
import com.robot.home.review.vo.ReviewVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 机器人评价服务实现
 */
@Service
public class RobotReviewServiceImpl extends ServiceImpl<RobotReviewMapper, RobotReview> implements RobotReviewService {

    private static final Logger log = LoggerFactory.getLogger(RobotReviewServiceImpl.class);

    @Resource
    private RobotReviewSummaryMapper summaryMapper;
    @Resource
    private RobotReviewHelpfulMapper helpfulMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private UserMapper userMapper;

    // ---- 评价状态常量 ----
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, ReviewDTO dto) {
        // 1. 校验机器人存在
        Robot robot = robotMapper.selectById(dto.getRobotId());
        if (robot == null) {
            throw new BusinessException("机器人不存在");
        }

        // 2. 防重复: 每个用户每个机器人仅一条评价
        Long existCount = count(Wrappers.<RobotReview>lambdaQuery()
                .eq(RobotReview::getRobotId, dto.getRobotId())
                .eq(RobotReview::getUserId, userId)
                .eq(RobotReview::getDeleted, Constants.DELETED_NO));
        if (existCount > 0) {
            throw new BusinessException("您已评价过该机器人，请修改已有评价");
        }

        // 3. 构建实体
        RobotReview review = new RobotReview();
        review.setRobotId(dto.getRobotId());
        review.setUserId(userId);
        review.setOverallScore(dto.getOverallScore());
        review.setQualityScore(dto.getQualityScore());
        review.setServiceScore(dto.getServiceScore());
        review.setCostScore(dto.getCostScore());
        review.setContent(XssUtils.escapeText(StrUtil.trim(dto.getContent())));
        review.setImages(dto.getImages());
        review.setStatus(STATUS_PENDING);
        review.setHelpfulCount(0);
        save(review);

        // 4. 更新汇总
        refreshSummary(dto.getRobotId());

        return review.getId();
    }

    @Override
    public PageResult<ReviewVO> list(Long robotId, Long currentUserId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotReview> page = new Page<>(pn, ps);
        IPage<RobotReview> result = page(page, Wrappers.<RobotReview>lambdaQuery()
                .eq(RobotReview::getRobotId, robotId)
                .eq(RobotReview::getStatus, STATUS_APPROVED)
                .eq(RobotReview::getDeleted, Constants.DELETED_NO)
                .orderByDesc(RobotReview::getHelpfulCount)
                .orderByDesc(RobotReview::getCreateTime));

        List<RobotReview> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(pn, ps, result.getTotal(), new ArrayList<>());
        }

        // Batch load users
        Set<Long> userIds = new HashSet<>();
        for (RobotReview r : records) {
            if (r.getUserId() != null) userIds.add(r.getUserId());
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        // Batch check helpful
        Set<Long> helpfuledIds = new HashSet<>();
        if (currentUserId != null) {
            List<Long> reviewIds = records.stream().map(RobotReview::getId).collect(Collectors.toList());
            List<RobotReviewHelpful> helpfuls = helpfulMapper.selectList(Wrappers.<RobotReviewHelpful>lambdaQuery()
                    .eq(RobotReviewHelpful::getUserId, currentUserId)
                    .in(RobotReviewHelpful::getReviewId, reviewIds));
            for (RobotReviewHelpful h : helpfuls) {
                helpfuledIds.add(h.getReviewId());
            }
        }

        List<ReviewVO> voList = records.stream()
                .map(r -> toVO(r, currentUserId, userMap, helpfuledIds))
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    @Override
    public ReviewSummaryVO summary(Long robotId) {
        RobotReviewSummary summary = summaryMapper.selectOne(
                Wrappers.<RobotReviewSummary>lambdaQuery()
                        .eq(RobotReviewSummary::getRobotId, robotId));
        if (summary == null) {
            // 返回空汇总
            ReviewSummaryVO vo = new ReviewSummaryVO();
            vo.setRobotId(robotId);
            vo.setReviewCount(0);
            vo.setOverallAvg(BigDecimal.ZERO);
            vo.setQualityAvg(BigDecimal.ZERO);
            vo.setServiceAvg(BigDecimal.ZERO);
            vo.setCostAvg(BigDecimal.ZERO);
            vo.setScore1Count(0);
            vo.setScore2Count(0);
            vo.setScore3Count(0);
            vo.setScore4Count(0);
            vo.setScore5Count(0);
            return vo;
        }
        return toSummaryVO(summary);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void helpful(Long userId, Long reviewId) {
        RobotReview review = getById(reviewId);
        if (review == null || review.getDeleted() == Constants.DELETED_YES) {
            throw new BusinessException("评价不存在");
        }

        // 防重复投票
        Long exist = helpfulMapper.selectCount(Wrappers.<RobotReviewHelpful>lambdaQuery()
                .eq(RobotReviewHelpful::getReviewId, reviewId)
                .eq(RobotReviewHelpful::getUserId, userId));
        if (exist > 0) {
            throw new BusinessException("您已标记过该评价");
        }

        // 插入投票记录
        RobotReviewHelpful h = new RobotReviewHelpful();
        h.setReviewId(reviewId);
        h.setUserId(userId);
        helpfulMapper.insert(h);

        // 更新helpful_count
        update(Wrappers.<RobotReview>lambdaUpdate()
                .eq(RobotReview::getId, reviewId)
                .setSql("helpful_count = helpful_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unhelpful(Long userId, Long reviewId) {
        int deleted = helpfulMapper.delete(Wrappers.<RobotReviewHelpful>lambdaQuery()
                .eq(RobotReviewHelpful::getReviewId, reviewId)
                .eq(RobotReviewHelpful::getUserId, userId));
        if (deleted > 0) {
            update(Wrappers.<RobotReview>lambdaUpdate()
                    .eq(RobotReview::getId, reviewId)
                    .setSql("helpful_count = GREATEST(helpful_count - 1, 0)"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long reviewId) {
        RobotReview review = getById(reviewId);
        if (review == null || review.getDeleted() == Constants.DELETED_YES) {
            throw new BusinessException("评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评价");
        }

        removeById(reviewId); // 逻辑删除（BaseEntity.deleted）
        refreshSummary(review.getRobotId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long reviewId, ReviewDTO dto) {
        RobotReview review = getById(reviewId);
        if (review == null || review.getDeleted() == Constants.DELETED_YES) {
            throw new BusinessException("评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("只能修改自己的评价");
        }

        review.setOverallScore(dto.getOverallScore());
        review.setQualityScore(dto.getQualityScore());
        review.setServiceScore(dto.getServiceScore());
        review.setCostScore(dto.getCostScore());
        review.setContent(XssUtils.escapeText(StrUtil.trim(dto.getContent())));
        review.setImages(dto.getImages());
        review.setStatus(STATUS_PENDING); // 修改后重新进入待审核
        updateById(review);

        refreshSummary(review.getRobotId());
    }

    // ---- 管理端 ----

    @Override
    public PageResult<ReviewVO> adminPage(Integer status, Long robotId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotReview> page = new Page<>(pn, ps);
        IPage<RobotReview> result = page(page, Wrappers.<RobotReview>lambdaQuery()
                .eq(status != null, RobotReview::getStatus, status)
                .eq(robotId != null, RobotReview::getRobotId, robotId)
                .eq(RobotReview::getDeleted, Constants.DELETED_NO)
                .orderByAsc(RobotReview::getStatus)  // 待审核排前面
                .orderByDesc(RobotReview::getCreateTime));
        List<ReviewVO> voList = result.getRecords().stream()
                .map(r -> toVO(r, null))
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long reviewId, Integer status, String reason) {
        if (status != STATUS_APPROVED && status != STATUS_REJECTED) {
            throw new BusinessException("审核状态无效");
        }
        RobotReview review = getById(reviewId);
        if (review == null || review.getDeleted() == Constants.DELETED_YES) {
            throw new BusinessException("评价不存在");
        }

        update(Wrappers.<RobotReview>lambdaUpdate()
                .eq(RobotReview::getId, reviewId)
                .set(RobotReview::getStatus, status)
                .set(StrUtil.isNotBlank(reason), RobotReview::getReplyContent, reason));

        refreshSummary(review.getRobotId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long reviewId, String content) {
        RobotReview review = getById(reviewId);
        if (review == null || review.getDeleted() == Constants.DELETED_YES) {
            throw new BusinessException("评价不存在");
        }

        update(Wrappers.<RobotReview>lambdaUpdate()
                .eq(RobotReview::getId, reviewId)
                .set(RobotReview::getReplyContent, XssUtils.escapeText(content))
                .set(RobotReview::getReplyTime, LocalDateTime.now()));
    }

    // ---- 内部方法 ----

    /**
     * 刷新机器人评价汇总（全量重算）
     */
    private void refreshSummary(Long robotId) {
        try {
            List<RobotReview> reviews = list(Wrappers.<RobotReview>lambdaQuery()
                    .eq(RobotReview::getRobotId, robotId)
                    .eq(RobotReview::getStatus, STATUS_APPROVED)
                    .eq(RobotReview::getDeleted, Constants.DELETED_NO));

            RobotReviewSummary summary = summaryMapper.selectOne(
                    Wrappers.<RobotReviewSummary>lambdaQuery()
                            .eq(RobotReviewSummary::getRobotId, robotId));

            if (summary == null) {
                summary = new RobotReviewSummary();
                summary.setRobotId(robotId);
            }

            int count = reviews.size();
            summary.setReviewCount(count);

            if (count == 0) {
                summary.setOverallAvg(BigDecimal.ZERO);
                summary.setQualityAvg(BigDecimal.ZERO);
                summary.setServiceAvg(BigDecimal.ZERO);
                summary.setCostAvg(BigDecimal.ZERO);
                summary.setScore1Count(0);
                summary.setScore2Count(0);
                summary.setScore3Count(0);
                summary.setScore4Count(0);
                summary.setScore5Count(0);
            } else {
                int sumOverall = 0, sumQuality = 0, sumService = 0, sumCost = 0;
                int[] stars = new int[6]; // index 1-5
                for (RobotReview r : reviews) {
                    sumOverall += r.getOverallScore();
                    sumQuality += r.getQualityScore();
                    sumService += r.getServiceScore();
                    sumCost += r.getCostScore();
                    if (r.getOverallScore() >= 1 && r.getOverallScore() <= 5) {
                        stars[r.getOverallScore()]++;
                    }
                }
                BigDecimal divisor = new BigDecimal(count);
                summary.setOverallAvg(new BigDecimal(sumOverall).divide(divisor, 1, RoundingMode.HALF_UP));
                summary.setQualityAvg(new BigDecimal(sumQuality).divide(divisor, 1, RoundingMode.HALF_UP));
                summary.setServiceAvg(new BigDecimal(sumService).divide(divisor, 1, RoundingMode.HALF_UP));
                summary.setCostAvg(new BigDecimal(sumCost).divide(divisor, 1, RoundingMode.HALF_UP));
                summary.setScore1Count(stars[1]);
                summary.setScore2Count(stars[2]);
                summary.setScore3Count(stars[3]);
                summary.setScore4Count(stars[4]);
                summary.setScore5Count(stars[5]);
            }

            if (summary.getId() == null) {
                summaryMapper.insert(summary);
            } else {
                summaryMapper.updateById(summary);
            }
        } catch (Exception e) {
            log.error("刷新评价汇总失败, robotId={}", robotId, e);
        }
    }

    /**
     * Entity -> VO (批量版本，用于列表)
     */
    private ReviewVO toVO(RobotReview review, Long currentUserId, Map<Long, User> userMap, Set<Long> helpfuledIds) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setRobotId(review.getRobotId());
        vo.setOverallScore(review.getOverallScore());
        vo.setQualityScore(review.getQualityScore());
        vo.setServiceScore(review.getServiceScore());
        vo.setCostScore(review.getCostScore());
        vo.setContent(review.getContent());
        vo.setHelpfulCount(review.getHelpfulCount());
        vo.setReplyContent(review.getReplyContent());
        vo.setReplyTime(review.getReplyTime());
        vo.setCreateTime(review.getCreateTime());
        vo.setStatus(review.getStatus());

        // 图片列表
        if (StrUtil.isNotBlank(review.getImages())) {
            vo.setImageList(Arrays.stream(review.getImages().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList()));
        } else {
            vo.setImageList(Collections.emptyList());
        }

        // 用户信息 (从批量缓存取)
        if (review.getUserId() != null) {
            User user = userMap.get(review.getUserId());
            if (user != null) {
                ReviewUserVO uvo = new ReviewUserVO();
                uvo.setId(user.getId());
                uvo.setNickname(user.getNickname());
                uvo.setAvatar(user.getAvatar());
                vo.setUser(uvo);
            }
        }

        // 是否已标记有用 (从批量缓存取)
        vo.setHelpfuled(currentUserId != null && helpfuledIds.contains(review.getId()));

        // 是否可编辑
        vo.setCanEdit(currentUserId != null && currentUserId.equals(review.getUserId()));

        return vo;
    }

    /**
     * Entity -> VO (单条版本，用于admin/detail)
     */
    private ReviewVO toVO(RobotReview review, Long currentUserId) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setRobotId(review.getRobotId());
        vo.setOverallScore(review.getOverallScore());
        vo.setQualityScore(review.getQualityScore());
        vo.setServiceScore(review.getServiceScore());
        vo.setCostScore(review.getCostScore());
        vo.setContent(review.getContent());
        vo.setHelpfulCount(review.getHelpfulCount());
        vo.setReplyContent(review.getReplyContent());
        vo.setReplyTime(review.getReplyTime());
        vo.setCreateTime(review.getCreateTime());
        vo.setStatus(review.getStatus());

        // 图片列表
        if (StrUtil.isNotBlank(review.getImages())) {
            vo.setImageList(Arrays.stream(review.getImages().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList()));
        } else {
            vo.setImageList(Collections.emptyList());
        }

        // 用户信息
        if (review.getUserId() != null) {
            User user = userMapper.selectById(review.getUserId());
            if (user != null) {
                ReviewUserVO uvo = new ReviewUserVO();
                uvo.setId(user.getId());
                uvo.setNickname(user.getNickname());
                uvo.setAvatar(user.getAvatar());
                vo.setUser(uvo);
            }
        }

        // 是否已标记有用
        if (currentUserId != null) {
            Long helpfulCount = helpfulMapper.selectCount(Wrappers.<RobotReviewHelpful>lambdaQuery()
                    .eq(RobotReviewHelpful::getReviewId, review.getId())
                    .eq(RobotReviewHelpful::getUserId, currentUserId));
            vo.setHelpfuled(helpfulCount > 0);
        } else {
            vo.setHelpfuled(false);
        }

        // 是否可编辑
        vo.setCanEdit(currentUserId != null && currentUserId.equals(review.getUserId()));

        return vo;
    }

    /**
     * Summary Entity -> VO
     */
    private ReviewSummaryVO toSummaryVO(RobotReviewSummary summary) {
        ReviewSummaryVO vo = new ReviewSummaryVO();
        vo.setRobotId(summary.getRobotId());
        vo.setReviewCount(summary.getReviewCount());
        vo.setOverallAvg(summary.getOverallAvg());
        vo.setQualityAvg(summary.getQualityAvg());
        vo.setServiceAvg(summary.getServiceAvg());
        vo.setCostAvg(summary.getCostAvg());
        vo.setScore1Count(summary.getScore1Count());
        vo.setScore2Count(summary.getScore2Count());
        vo.setScore3Count(summary.getScore3Count());
        vo.setScore4Count(summary.getScore4Count());
        vo.setScore5Count(summary.getScore5Count());
        return vo;
    }
}