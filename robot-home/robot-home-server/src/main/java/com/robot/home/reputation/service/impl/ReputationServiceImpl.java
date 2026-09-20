package com.robot.home.reputation.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.reputation.entity.ReputationEvent;
import com.robot.home.reputation.entity.UserReputation;
import com.robot.home.reputation.mapper.ReputationEventMapper;
import com.robot.home.reputation.mapper.UserReputationMapper;
import com.robot.home.reputation.service.ReputationService;
import com.robot.home.reputation.vo.ReputationEventVO;
import com.robot.home.reputation.vo.ReputationVO;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * P0-7: 贡献信誉服务实现
 * <p>
 * 核心逻辑:
 * - 信誉分 = Σ(正向事件加分) + Σ(负向事件扣分)
 * - 等级阈值: NEW(0), CONTRIBUTOR(30), ACTIVE(100), TRUSTED(300), EXPERT(800)
 * - eventKey幂等: SHA-256(userId+eventType+referenceType+referenceId)
 * - 每日贡献上限: 默认200分/天，防刷
 * - Redis缓存: key=robot:reputation:{userId}, TTL=300s
 * <p>
 * 安全措施:
 * - IDOR: 所有用户端接口通过userId参数校验
 * - 防刷: eventKey去重 + 每日贡献上限
 * - XSS: 管理端reason字段escapeText
 */
@Service
public class ReputationServiceImpl implements ReputationService {

    private static final Logger log = LoggerFactory.getLogger(ReputationServiceImpl.class);

    private static final long CACHE_SECONDS = 300L;

    /** 每日贡献上限（防刷） */
    private static final int DAILY_CONTRIBUTION_CAP = 200;

    /** 事件类型白名单 */
    private static final Set<String> VALID_EVENT_TYPES = new HashSet<>(Arrays.asList(
            Constants.REP_EVENT_CORRECTION_ACCEPTED,
            Constants.REP_EVENT_ANSWER_ACCEPTED,
            Constants.REP_EVENT_REVIEW_HELPFUL,
            Constants.REP_EVENT_POST_QUALITY,
            Constants.REP_EVENT_QUESTION_ANSWERED,
            Constants.REP_EVENT_ABUSE_PENALTY,
            Constants.REP_EVENT_SPAM_REJECTED
    ));

    /** 事件类型→分数映射 */
    private static final Map<String, Integer> EVENT_SCORE_MAP = new HashMap<>();

    static {
        EVENT_SCORE_MAP.put(Constants.REP_EVENT_CORRECTION_ACCEPTED, Constants.REP_SCORE_CORRECTION_ACCEPTED);
        EVENT_SCORE_MAP.put(Constants.REP_EVENT_ANSWER_ACCEPTED, Constants.REP_SCORE_ANSWER_ACCEPTED);
        EVENT_SCORE_MAP.put(Constants.REP_EVENT_REVIEW_HELPFUL, Constants.REP_SCORE_REVIEW_HELPFUL);
        EVENT_SCORE_MAP.put(Constants.REP_EVENT_POST_QUALITY, Constants.REP_SCORE_POST_QUALITY);
        EVENT_SCORE_MAP.put(Constants.REP_EVENT_QUESTION_ANSWERED, Constants.REP_SCORE_QUESTION_ANSWERED);
        EVENT_SCORE_MAP.put(Constants.REP_EVENT_ABUSE_PENALTY, Constants.REP_SCORE_ABUSE_PENALTY);
        EVENT_SCORE_MAP.put(Constants.REP_EVENT_SPAM_REJECTED, Constants.REP_SCORE_SPAM_REJECTED);
    }

    /** 事件类型→中文 */
    private static final Map<String, String> EVENT_LABEL_MAP = new HashMap<>();

    static {
        EVENT_LABEL_MAP.put(Constants.REP_EVENT_CORRECTION_ACCEPTED, "纠错被采纳");
        EVENT_LABEL_MAP.put(Constants.REP_EVENT_ANSWER_ACCEPTED, "回答被采纳");
        EVENT_LABEL_MAP.put(Constants.REP_EVENT_REVIEW_HELPFUL, "评测有帮助");
        EVENT_LABEL_MAP.put(Constants.REP_EVENT_POST_QUALITY, "高质量帖子");
        EVENT_LABEL_MAP.put(Constants.REP_EVENT_QUESTION_ANSWERED, "回答问题");
        EVENT_LABEL_MAP.put(Constants.REP_EVENT_ABUSE_PENALTY, "滥用惩罚");
        EVENT_LABEL_MAP.put(Constants.REP_EVENT_SPAM_REJECTED, "垃圾内容惩罚");
    }

    /** 等级→中文 */
    private static final Map<String, String> LEVEL_LABEL_MAP = new HashMap<>();

    static {
        LEVEL_LABEL_MAP.put(Constants.REP_NEW, "新手");
        LEVEL_LABEL_MAP.put(Constants.REP_CONTRIBUTOR, "贡献者");
        LEVEL_LABEL_MAP.put(Constants.REP_ACTIVE, "活跃用户");
        LEVEL_LABEL_MAP.put(Constants.REP_TRUSTED, "可信用户");
        LEVEL_LABEL_MAP.put(Constants.REP_EXPERT, "专家");
    }

    /** 等级阈值（从小到大） */
    private static final int[] LEVEL_THRESHOLDS = {
            0,                          // NEW
            Constants.REP_THRESHOLD_CONTRIBUTOR,  // 30
            Constants.REP_THRESHOLD_ACTIVE,       // 100
            Constants.REP_THRESHOLD_TRUSTED,      // 300
            Constants.REP_THRESHOLD_EXPERT         // 800
    };
    private static final String[] LEVEL_NAMES = {
            Constants.REP_NEW,
            Constants.REP_CONTRIBUTOR,
            Constants.REP_ACTIVE,
            Constants.REP_TRUSTED,
            Constants.REP_EXPERT
    };

    @Resource
    private UserReputationMapper userReputationMapper;
    @Resource
    private ReputationEventMapper reputationEventMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public ReputationVO getReputation(Long userId) {
        // 尝试Redis缓存
        String cacheKey = Constants.CACHE_REPUTATION_PREFIX + userId;
        try {
            ReputationVO cached = redisUtils.getObj(cacheKey, ReputationVO.class);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis信誉缓存读取失败，降级到DB: userId={}, error={}", userId, e.getMessage());
        }

        UserReputation rep = getOrCreateReputation(userId);
        ReputationVO vo = toVO(rep);

        // 填充最近5条事件
        List<ReputationEvent> recentEvents = reputationEventMapper.selectList(
                Wrappers.<ReputationEvent>lambdaQuery()
                        .eq(ReputationEvent::getUserId, userId)
                        .orderByDesc(ReputationEvent::getCreateTime)
                        .last("LIMIT 5"));
        vo.setRecentEvents(recentEvents.stream().map(this::toEventVO).collect(Collectors.toList()));

        // 写入缓存
        try {
            redisUtils.setObj(cacheKey, vo, CACHE_SECONDS + (long) (Math.random() * 30), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis信誉缓存写入失败（不影响返回）: userId={}, error={}", userId, e.getMessage());
        }

        return vo;
    }

    @Override
    public PageResult<ReputationEventVO> getEventHistory(Long userId, String eventType,
                                                          Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : Math.min(pageNum, 1000);
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Page<ReputationEvent> page = new Page<>(pn, ps);
        IPage<ReputationEvent> result = reputationEventMapper.selectPage(page,
                Wrappers.<ReputationEvent>lambdaQuery()
                        .eq(ReputationEvent::getUserId, userId)
                        .eq(StrUtil.isNotBlank(eventType), ReputationEvent::getEventType, eventType)
                        .orderByDesc(ReputationEvent::getCreateTime));
        List<ReputationEventVO> vos = result.getRecords().stream()
                .map(this::toEventVO)
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordEvent(Long userId, String eventType, String eventKey,
                            String referenceType, Long referenceId) {
        // 1. 类型白名单校验
        if (!VALID_EVENT_TYPES.contains(eventType)) {
            log.warn("信誉事件类型不在白名单: eventType={}", eventType);
            return;
        }

        // 2. eventKey幂等（如果调用方没提供eventKey，自动生成）
        String finalEventKey = eventKey;
        if (StrUtil.isBlank(finalEventKey)) {
            finalEventKey = SecureUtil.sha256(userId + ":" + eventType + ":" + referenceType + ":" + referenceId);
        }

        // 3. 先查是否已存在（应用层去重）
        Long exists = reputationEventMapper.selectCount(
                Wrappers.<ReputationEvent>lambdaQuery()
                        .eq(ReputationEvent::getEventKey, finalEventKey));
        if (exists > 0) {
            log.debug("信誉事件已存在，跳过: eventKey={}", finalEventKey);
            return;
        }

        // 4. 每日贡献上限检查（仅正向事件）
        int scoreDelta = getScoreDelta(eventType);
        if (scoreDelta > 0 && !checkDailyCap(userId)) {
            log.info("用户{}今日信誉贡献已达上限，跳过正向事件: eventType={}", userId, eventType);
            return;
        }

        // 5. 插入事件记录
        ReputationEvent event = new ReputationEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setEventKey(finalEventKey);
        event.setScoreDelta(scoreDelta);
        event.setReferenceType(referenceType);
        event.setReferenceId(referenceId);
        event.setCreateTime(LocalDateTime.now());
        try {
            reputationEventMapper.insert(event);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底
            log.debug("信誉事件唯一索引冲突，跳过: eventKey={}", finalEventKey);
            return;
        }

        // 6. 更新用户信誉分
        UserReputation rep = getOrCreateReputation(userId);
        rep.setReputationScore(Math.max(0, rep.getReputationScore() + scoreDelta));
        rep.setReputationLevel(calculateLevel(rep.getReputationScore()));
        updateCounters(rep, eventType, scoreDelta > 0 ? 1 : 0);
        rep.setUpdateTime(LocalDateTime.now());
        userReputationMapper.updateById(rep);

        // 7. 同步到User表（冗余字段，方便查询排序）
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setReputationScore(rep.getReputationScore());
            user.setReputationLevel(rep.getReputationLevel());
            userMapper.updateById(user);
        }

        // 8. 清除缓存
        invalidateCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculate(Long userId) {
        UserReputation rep = getOrCreateReputation(userId);

        // 从事件表重算总分
        List<ReputationEvent> events = reputationEventMapper.selectList(
                Wrappers.<ReputationEvent>lambdaQuery()
                        .eq(ReputationEvent::getUserId, userId));
        int totalScore = events.stream()
                .mapToInt(ReputationEvent::getScoreDelta)
                .sum();
        totalScore = Math.max(0, totalScore);

        rep.setReputationScore(totalScore);
        rep.setReputationLevel(calculateLevel(totalScore));

        // 重算统计计数
        int acceptedCorrections = 0, acceptedAnswers = 0, helpfulReviews = 0, qualityPosts = 0, penaltyScore = 0;
        for (ReputationEvent e : events) {
            if (Constants.REP_EVENT_CORRECTION_ACCEPTED.equals(e.getEventType())) acceptedCorrections++;
            else if (Constants.REP_EVENT_ANSWER_ACCEPTED.equals(e.getEventType())) acceptedAnswers++;
            else if (Constants.REP_EVENT_REVIEW_HELPFUL.equals(e.getEventType())) helpfulReviews++;
            else if (Constants.REP_EVENT_POST_QUALITY.equals(e.getEventType())) qualityPosts++;
            else if (Constants.REP_EVENT_ABUSE_PENALTY.equals(e.getEventType())
                    || Constants.REP_EVENT_SPAM_REJECTED.equals(e.getEventType())) {
                penaltyScore += Math.abs(e.getScoreDelta());
            }
        }
        rep.setAcceptedCorrections(acceptedCorrections);
        rep.setAcceptedAnswers(acceptedAnswers);
        rep.setHelpfulReviews(helpfulReviews);
        rep.setQualityPosts(qualityPosts);
        rep.setPenaltyScore(penaltyScore);
        rep.setUpdateTime(LocalDateTime.now());
        userReputationMapper.updateById(rep);

        // 同步User表
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setReputationScore(totalScore);
            user.setReputationLevel(calculateLevel(totalScore));
            userMapper.updateById(user);
        }

        invalidateCache(userId);
    }

    @Override
    public void batchRecalculate(List<Long> userIds) {
        for (Long userId : userIds) {
            try {
                recalculate(userId);
            } catch (Exception e) {
                log.error("批量重算信誉失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    @Override
    public PageResult<ReputationVO> adminListByLevel(String reputationLevel,
                                                      Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : Math.min(pageNum, 1000);
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Page<UserReputation> page = new Page<>(pn, ps);
        IPage<UserReputation> result = userReputationMapper.selectPage(page,
                Wrappers.<UserReputation>lambdaQuery()
                        .eq(StrUtil.isNotBlank(reputationLevel),
                                UserReputation::getReputationLevel, reputationLevel)
                        .orderByDesc(UserReputation::getReputationScore));
        List<ReputationVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAdjustScore(Long userId, int delta, String reason) {
        UserReputation rep = getOrCreateReputation(userId);
        rep.setReputationScore(Math.max(0, rep.getReputationScore() + delta));
        rep.setReputationLevel(calculateLevel(rep.getReputationScore()));
        if (delta < 0) {
            rep.setPenaltyScore(rep.getPenaltyScore() + Math.abs(delta));
        }
        rep.setUpdateTime(LocalDateTime.now());
        userReputationMapper.updateById(rep);

        // 记录事件
        ReputationEvent event = new ReputationEvent();
        event.setUserId(userId);
        event.setEventType(delta > 0 ? "ADMIN_BONUS" : "ADMIN_PENALTY");
        event.setEventKey(SecureUtil.sha256("admin:" + userId + ":" + System.currentTimeMillis()));
        event.setScoreDelta(delta);
        event.setReferenceType("ADMIN");
        event.setReferenceId(0L);
        event.setCreateTime(LocalDateTime.now());
        reputationEventMapper.insert(event);

        // 同步User表
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setReputationScore(rep.getReputationScore());
            user.setReputationLevel(rep.getReputationLevel());
            userMapper.updateById(user);
        }

        invalidateCache(userId);
    }

    @Override
    public boolean checkDailyCap(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // 查今日正向事件总分
        List<ReputationEvent> todayEvents = reputationEventMapper.selectList(
                Wrappers.<ReputationEvent>lambdaQuery()
                        .eq(ReputationEvent::getUserId, userId)
                        .ge(ReputationEvent::getCreateTime, startOfDay)
                        .lt(ReputationEvent::getCreateTime, endOfDay)
                        .gt(ReputationEvent::getScoreDelta, 0));
        int todayPositiveScore = todayEvents.stream()
                .mapToInt(ReputationEvent::getScoreDelta)
                .sum();
        return todayPositiveScore < DAILY_CONTRIBUTION_CAP;
    }

    // ========== 内部方法 ==========

    private UserReputation getOrCreateReputation(Long userId) {
        UserReputation rep = userReputationMapper.selectOne(
                Wrappers.<UserReputation>lambdaQuery()
                        .eq(UserReputation::getUserId, userId));
        if (rep == null) {
            rep = new UserReputation();
            rep.setUserId(userId);
            rep.setReputationScore(0);
            rep.setReputationLevel(Constants.REP_NEW);
            rep.setAcceptedCorrections(0);
            rep.setAcceptedAnswers(0);
            rep.setHelpfulReviews(0);
            rep.setQualityPosts(0);
            rep.setPenaltyScore(0);
            rep.setDailyContribCap(DAILY_CONTRIBUTION_CAP);
            rep.setUpdateTime(LocalDateTime.now());
            try {
                userReputationMapper.insert(rep);
            } catch (DuplicateKeyException e) {
                // 并发创建兜底
                rep = userReputationMapper.selectOne(
                        Wrappers.<UserReputation>lambdaQuery()
                                .eq(UserReputation::getUserId, userId));
            }
        }
        return rep;
    }

    private String calculateLevel(int score) {
        String level = Constants.REP_NEW;
        for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
            if (score >= LEVEL_THRESHOLDS[i]) {
                level = LEVEL_NAMES[i];
            }
        }
        return level;
    }

    private int getScoreDelta(String eventType) {
        Integer delta = EVENT_SCORE_MAP.get(eventType);
        return delta != null ? delta : 0;
    }

    private void updateCounters(UserReputation rep, String eventType, int increment) {
        if (Constants.REP_EVENT_CORRECTION_ACCEPTED.equals(eventType)) {
            rep.setAcceptedCorrections(rep.getAcceptedCorrections() + increment);
        } else if (Constants.REP_EVENT_ANSWER_ACCEPTED.equals(eventType)) {
            rep.setAcceptedAnswers(rep.getAcceptedAnswers() + increment);
        } else if (Constants.REP_EVENT_REVIEW_HELPFUL.equals(eventType)) {
            rep.setHelpfulReviews(rep.getHelpfulReviews() + increment);
        } else if (Constants.REP_EVENT_POST_QUALITY.equals(eventType)) {
            rep.setQualityPosts(rep.getQualityPosts() + increment);
        } else if (Constants.REP_EVENT_ABUSE_PENALTY.equals(eventType)
                || Constants.REP_EVENT_SPAM_REJECTED.equals(eventType)) {
            rep.setPenaltyScore(rep.getPenaltyScore() + Math.abs(getScoreDelta(eventType)));
        }
    }

    private ReputationVO toVO(UserReputation rep) {
        ReputationVO vo = new ReputationVO();
        vo.setUserId(rep.getUserId());
        vo.setReputationScore(rep.getReputationScore());
        vo.setReputationLevel(rep.getReputationLevel());
        vo.setLevelLabel(LEVEL_LABEL_MAP.getOrDefault(rep.getReputationLevel(), "未知"));
        vo.setAcceptedCorrections(rep.getAcceptedCorrections());
        vo.setAcceptedAnswers(rep.getAcceptedAnswers());
        vo.setHelpfulReviews(rep.getHelpfulReviews());
        vo.setQualityPosts(rep.getQualityPosts());
        vo.setPenaltyScore(rep.getPenaltyScore());
        vo.setUpdateTime(rep.getUpdateTime());

        // 计算下一等级信息
        int score = rep.getReputationScore();
        int nextThreshold = 0;
        for (int threshold : LEVEL_THRESHOLDS) {
            if (score < threshold) {
                nextThreshold = threshold;
                break;
            }
        }
        vo.setNextLevelScore(nextThreshold);
        vo.setScoreToNextLevel(nextThreshold > 0 ? Math.max(0, nextThreshold - score) : 0);

        // 填充用户信息
        User user = userMapper.selectById(rep.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        return vo;
    }

    private ReputationEventVO toEventVO(ReputationEvent event) {
        ReputationEventVO vo = new ReputationEventVO();
        vo.setId(event.getId());
        vo.setUserId(event.getUserId());
        vo.setEventType(event.getEventType());
        vo.setEventTypeLabel(EVENT_LABEL_MAP.getOrDefault(event.getEventType(), event.getEventType()));
        vo.setScoreDelta(event.getScoreDelta());
        vo.setReferenceType(event.getReferenceType());
        vo.setReferenceId(event.getReferenceId());
        vo.setCreateTime(event.getCreateTime());
        return vo;
    }

    private void invalidateCache(Long userId) {
        try {
            redisUtils.delete(Constants.CACHE_REPUTATION_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis信誉缓存清除失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}