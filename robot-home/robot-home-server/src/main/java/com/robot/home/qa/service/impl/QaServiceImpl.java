package com.robot.home.qa.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.exception.PermissionException;
import com.robot.home.common.service.BizCounter;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.qa.dto.AnswerDTO;
import com.robot.home.qa.dto.QuestionDTO;
import com.robot.home.qa.entity.AnswerHelpful;
import com.robot.home.qa.entity.QuestionFollow;
import com.robot.home.qa.entity.RobotAnswer;
import com.robot.home.qa.entity.RobotQuestion;
import com.robot.home.qa.mapper.AnswerHelpfulMapper;
import com.robot.home.qa.mapper.QuestionFollowMapper;
import com.robot.home.qa.mapper.RobotAnswerMapper;
import com.robot.home.qa.mapper.RobotQuestionMapper;
import com.robot.home.qa.service.QaService;
import com.robot.home.qa.vo.AnswerVO;
import com.robot.home.qa.vo.QuestionVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QaServiceImpl extends ServiceImpl<RobotQuestionMapper, RobotQuestion> implements QaService {

    private static final Logger log = LoggerFactory.getLogger(QaServiceImpl.class);
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_HIDDEN = 2;
    private static final int STATUS_DELETED = 3;

    @Resource
    private RobotAnswerMapper answerMapper;
    @Resource
    private QuestionFollowMapper questionFollowMapper;
    @Resource
    private AnswerHelpfulMapper helpfulMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private BizCounter bizCounter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long ask(Long userId, QuestionDTO dto) {
        if (StrUtil.isBlank(dto.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        RobotQuestion q = new RobotQuestion();
        q.setUserId(userId);
        q.setRobotId(dto.getRobotId());
        q.setTitle(XssUtils.clean(StrUtil.trim(dto.getTitle())));
        q.setContent(StrUtil.isBlank(dto.getContent()) ? null : XssUtils.clean(dto.getContent()));
        q.setStatus(STATUS_PUBLISHED);
        q.setAnswerCount(0);
        q.setFollowCount(0);
        q.setViewCount(0);
        q.setHasAccepted(0);
        save(q);
        return q.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long answer(Long userId, Long questionId, AnswerDTO dto) {
        RobotQuestion q = getById(questionId);
        if (q == null || q.getStatus() == STATUS_DELETED) {
            throw new BusinessException("问题不存在");
        }
        RobotAnswer a = new RobotAnswer();
        a.setQuestionId(questionId);
        a.setUserId(userId);
        a.setContent(XssUtils.clean(dto.getContent()));
        a.setHelpfulCount(0);
        a.setAccepted(0);
        a.setStatus(STATUS_PUBLISHED);
        answerMapper.insert(a);
        // 更新回答数
        update(Wrappers.<RobotQuestion>lambdaUpdate()
                .eq(RobotQuestion::getId, questionId)
                .setSql("answer_count = answer_count + 1"));
        return a.getId();
    }

    @Override
    public PageResult<QuestionVO> questions(Long robotId, String sort, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotQuestion> page = new Page<>(pn, ps);
        IPage<RobotQuestion> result = page(page, Wrappers.<RobotQuestion>lambdaQuery()
                .eq(RobotQuestion::getStatus, STATUS_PUBLISHED)
                .eq(robotId != null, RobotQuestion::getRobotId, robotId)
                .orderByDesc("hot".equals(sort), RobotQuestion::getFollowCount)
                .orderByDesc(RobotQuestion::getCreateTime));
        return toQuestionPageResult(result, null, pn, ps);
    }

    @Override
    public QuestionVO questionDetail(Long id, Long currentUserId) {
        RobotQuestion q = getById(id);
        if (q == null || q.getStatus() == STATUS_DELETED) {
            throw new BusinessException("问题不存在");
        }
        bizCounter.incr("question", id, BizCounter.Field.VIEW);
        return toQuestionVO(q, currentUserId);
    }

    @Override
    public PageResult<AnswerVO> answers(Long questionId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotAnswer> page = new Page<>(pn, ps);
        IPage<RobotAnswer> result = answerMapper.selectPage(page, Wrappers.<RobotAnswer>lambdaQuery()
                .eq(RobotAnswer::getQuestionId, questionId)
                .eq(RobotAnswer::getStatus, STATUS_PUBLISHED)
                .orderByDesc(RobotAnswer::getAccepted)
                .orderByDesc(RobotAnswer::getHelpfulCount)
                .orderByDesc(RobotAnswer::getCreateTime));
        return toAnswerPageResult(result, null, pn, ps);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long userId, Long questionId) {
        Long count = questionFollowMapper.selectCount(Wrappers.<QuestionFollow>lambdaQuery()
                .eq(QuestionFollow::getUserId, userId)
                .eq(QuestionFollow::getQuestionId, questionId));
        if (count > 0) {
            return;
        }
        QuestionFollow f = new QuestionFollow();
        f.setUserId(userId);
        f.setQuestionId(questionId);
        questionFollowMapper.insert(f);
        update(Wrappers.<RobotQuestion>lambdaUpdate()
                .eq(RobotQuestion::getId, questionId)
                .setSql("follow_count = follow_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long userId, Long questionId) {
        int deleted = questionFollowMapper.delete(Wrappers.<QuestionFollow>lambdaQuery()
                .eq(QuestionFollow::getUserId, userId)
                .eq(QuestionFollow::getQuestionId, questionId));
        if (deleted > 0) {
            update(Wrappers.<RobotQuestion>lambdaUpdate()
                    .eq(RobotQuestion::getId, questionId)
                    .setSql("follow_count = GREATEST(follow_count - 1, 0)"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void helpful(Long userId, Long answerId) {
        Long count = helpfulMapper.selectCount(Wrappers.<AnswerHelpful>lambdaQuery()
                .eq(AnswerHelpful::getUserId, userId)
                .eq(AnswerHelpful::getAnswerId, answerId));
        if (count > 0) {
            return;
        }
        AnswerHelpful h = new AnswerHelpful();
        h.setUserId(userId);
        h.setAnswerId(answerId);
        helpfulMapper.insert(h);
        answerMapper.update(null, Wrappers.<RobotAnswer>lambdaUpdate()
                .eq(RobotAnswer::getId, answerId)
                .setSql("helpful_count = helpful_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unhelpful(Long userId, Long answerId) {
        int deleted = helpfulMapper.delete(Wrappers.<AnswerHelpful>lambdaQuery()
                .eq(AnswerHelpful::getUserId, userId)
                .eq(AnswerHelpful::getAnswerId, answerId));
        if (deleted > 0) {
            answerMapper.update(null, Wrappers.<RobotAnswer>lambdaUpdate()
                    .eq(RobotAnswer::getId, answerId)
                    .setSql("helpful_count = GREATEST(helpful_count - 1, 0)"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long userId, Long answerId, boolean isAdmin) {
        RobotAnswer a = answerMapper.selectById(answerId);
        if (a == null || a.getStatus() == STATUS_DELETED) {
            throw new BusinessException("回答不存在");
        }
        RobotQuestion q = getById(a.getQuestionId());
        if (q == null) {
            throw new BusinessException("问题不存在");
        }
        if (!isAdmin && !q.getUserId().equals(userId)) {
            throw new PermissionException("只有提问者可以采纳回答");
        }
        // 取消之前的采纳
        answerMapper.update(null, Wrappers.<RobotAnswer>lambdaUpdate()
                .eq(RobotAnswer::getQuestionId, a.getQuestionId())
                .eq(RobotAnswer::getAccepted, 1)
                .set(RobotAnswer::getAccepted, 0));
        // 采纳当前回答
        answerMapper.update(null, Wrappers.<RobotAnswer>lambdaUpdate()
                .eq(RobotAnswer::getId, answerId)
                .set(RobotAnswer::getAccepted, 1));
        // 更新问题hasAccepted
        update(Wrappers.<RobotQuestion>lambdaUpdate()
                .eq(RobotQuestion::getId, a.getQuestionId())
                .set(RobotQuestion::getHasAccepted, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long userId, Long questionId, boolean isAdmin) {
        RobotQuestion q = getById(questionId);
        if (q == null) {
            throw new BusinessException("问题不存在");
        }
        if (!isAdmin && !q.getUserId().equals(userId)) {
            throw new PermissionException("只能删除自己的问题");
        }
        update(Wrappers.<RobotQuestion>lambdaUpdate()
                .eq(RobotQuestion::getId, questionId)
                .set(RobotQuestion::getStatus, STATUS_DELETED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnswer(Long userId, Long answerId, boolean isAdmin) {
        RobotAnswer a = answerMapper.selectById(answerId);
        if (a == null) {
            throw new BusinessException("回答不存在");
        }
        if (!isAdmin && !a.getUserId().equals(userId)) {
            throw new PermissionException("只能删除自己的回答");
        }
        answerMapper.update(null, Wrappers.<RobotAnswer>lambdaUpdate()
                .eq(RobotAnswer::getId, answerId)
                .set(RobotAnswer::getStatus, STATUS_DELETED));
    }

    @Override
    public PageResult<QuestionVO> adminQuestions(Integer status, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotQuestion> page = new Page<>(pn, ps);
        IPage<RobotQuestion> result = page(page, Wrappers.<RobotQuestion>lambdaQuery()
                .eq(status != null, RobotQuestion::getStatus, status)
                .orderByDesc(RobotQuestion::getCreateTime));
        return toQuestionPageResult(result, null, pn, ps);
    }

    @Override
    public void updateQuestionStatus(Long questionId, Integer status) {
        update(Wrappers.<RobotQuestion>lambdaUpdate()
                .eq(RobotQuestion::getId, questionId)
                .set(RobotQuestion::getStatus, status));
    }

    @Override
    public PageResult<AnswerVO> adminAnswers(Long questionId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotAnswer> page = new Page<>(pn, ps);
        IPage<RobotAnswer> result = answerMapper.selectPage(page, Wrappers.<RobotAnswer>lambdaQuery()
                .eq(questionId != null, RobotAnswer::getQuestionId, questionId)
                .orderByDesc(RobotAnswer::getCreateTime));
        return toAnswerPageResult(result, null, pn, ps);
    }

    @Override
    public void updateAnswerStatus(Long answerId, Integer status) {
        answerMapper.update(null, Wrappers.<RobotAnswer>lambdaUpdate()
                .eq(RobotAnswer::getId, answerId)
                .set(RobotAnswer::getStatus, status));
    }

    // ===== Private helpers =====

    private PageResult<QuestionVO> toQuestionPageResult(IPage<RobotQuestion> result, Long currentUserId, int pn, int ps) {
        List<RobotQuestion> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(pn, ps, result.getTotal(), new ArrayList<QuestionVO>());
        }
        // Batch load users
        Set<Long> userIds = new HashSet<>();
        Set<Long> robotIds = new HashSet<>();
        for (RobotQuestion q : records) {
            if (q.getUserId() != null) userIds.add(q.getUserId());
            if (q.getRobotId() != null) robotIds.add(q.getRobotId());
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }
        Map<Long, Robot> robotMap = new HashMap<>();
        if (!robotIds.isEmpty()) {
            robotMapper.selectBatchIds(robotIds).forEach(r -> robotMap.put(r.getId(), r));
        }
        // Batch check follows
        Set<Long> followedIds = new HashSet<>();
        if (currentUserId != null) {
            List<QuestionFollow> follows = questionFollowMapper.selectList(Wrappers.<QuestionFollow>lambdaQuery()
                    .eq(QuestionFollow::getUserId, currentUserId)
                    .in(QuestionFollow::getQuestionId,
                            records.stream().map(RobotQuestion::getId).collect(Collectors.toList())));
            for (QuestionFollow f : follows) {
                followedIds.add(f.getQuestionId());
            }
        }
        List<QuestionVO> vos = records.stream().map(q -> {
            QuestionVO vo = new QuestionVO();
            vo.setId(q.getId());
            vo.setUserId(q.getUserId());
            vo.setRobotId(q.getRobotId());
            vo.setTitle(q.getTitle());
            vo.setContent(q.getContent());
            vo.setStatus(q.getStatus());
            vo.setAnswerCount(q.getAnswerCount());
            vo.setFollowCount(q.getFollowCount());
            vo.setViewCount(q.getViewCount());
            vo.setHasAccepted(q.getHasAccepted());
            vo.setCreateTime(q.getCreateTime());
            User u = userMap.get(q.getUserId());
            if (u != null) {
                vo.setAuthorNickname(StrUtil.isNotBlank(u.getNickname()) ? u.getNickname() : u.getUsername());
                vo.setAuthorAvatar(u.getAvatar());
            }
            Robot r = robotMap.get(q.getRobotId());
            if (r != null) {
                vo.setRobotName(r.getName());
            }
            vo.setFollowed(currentUserId != null && followedIds.contains(q.getId()));
            vo.setCanEdit(currentUserId != null && currentUserId.equals(q.getUserId()));
            vo.setCanDelete(currentUserId != null && currentUserId.equals(q.getUserId()));
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    private QuestionVO toQuestionVO(RobotQuestion q, Long currentUserId) {
        QuestionVO vo = new QuestionVO();
        vo.setId(q.getId());
        vo.setUserId(q.getUserId());
        vo.setRobotId(q.getRobotId());
        vo.setTitle(q.getTitle());
        vo.setContent(q.getContent());
        vo.setStatus(q.getStatus());
        vo.setAnswerCount(q.getAnswerCount());
        vo.setFollowCount(q.getFollowCount());
        vo.setViewCount(q.getViewCount());
        vo.setHasAccepted(q.getHasAccepted());
        vo.setCreateTime(q.getCreateTime());
        if (q.getUserId() != null) {
            User u = userMapper.selectById(q.getUserId());
            if (u != null) {
                vo.setAuthorNickname(StrUtil.isNotBlank(u.getNickname()) ? u.getNickname() : u.getUsername());
                vo.setAuthorAvatar(u.getAvatar());
            }
        }
        if (q.getRobotId() != null) {
            Robot r = robotMapper.selectById(q.getRobotId());
            if (r != null) {
                vo.setRobotName(r.getName());
            }
        }
        if (currentUserId != null) {
            Long followCount = questionFollowMapper.selectCount(Wrappers.<QuestionFollow>lambdaQuery()
                    .eq(QuestionFollow::getUserId, currentUserId)
                    .eq(QuestionFollow::getQuestionId, q.getId()));
            vo.setFollowed(followCount > 0);
            vo.setCanEdit(currentUserId.equals(q.getUserId()));
            vo.setCanDelete(currentUserId.equals(q.getUserId()));
        } else {
            vo.setFollowed(false);
            vo.setCanEdit(false);
            vo.setCanDelete(false);
        }
        return vo;
    }

    private PageResult<AnswerVO> toAnswerPageResult(IPage<RobotAnswer> result, Long currentUserId, int pn, int ps) {
        List<RobotAnswer> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(pn, ps, result.getTotal(), new ArrayList<AnswerVO>());
        }
        Set<Long> userIds = new HashSet<>();
        for (RobotAnswer a : records) {
            if (a.getUserId() != null) userIds.add(a.getUserId());
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }
        // Batch check helpful
        Set<Long> helpfuledIds = new HashSet<>();
        if (currentUserId != null) {
            List<AnswerHelpful> helpfuls = helpfulMapper.selectList(Wrappers.<AnswerHelpful>lambdaQuery()
                    .eq(AnswerHelpful::getUserId, currentUserId)
                    .in(AnswerHelpful::getAnswerId,
                            records.stream().map(RobotAnswer::getId).collect(Collectors.toList())));
            for (AnswerHelpful h : helpfuls) {
                helpfuledIds.add(h.getAnswerId());
            }
        }
        // Get question owner for canAccept
        Map<Long, Long> questionOwnerMap = new HashMap<>();
        Set<Long> questionIds = records.stream().map(RobotAnswer::getQuestionId).collect(Collectors.toSet());
        if (!questionIds.isEmpty()) {
            listByIds(questionIds).forEach(q -> questionOwnerMap.put(q.getId(), q.getUserId()));
        }
        List<AnswerVO> vos = records.stream().map(a -> {
            AnswerVO vo = new AnswerVO();
            vo.setId(a.getId());
            vo.setQuestionId(a.getQuestionId());
            vo.setUserId(a.getUserId());
            vo.setContent(a.getContent());
            vo.setHelpfulCount(a.getHelpfulCount());
            vo.setAccepted(a.getAccepted());
            vo.setStatus(a.getStatus());
            vo.setCreateTime(a.getCreateTime());
            User u = userMap.get(a.getUserId());
            if (u != null) {
                vo.setAuthorNickname(StrUtil.isNotBlank(u.getNickname()) ? u.getNickname() : u.getUsername());
                vo.setAuthorAvatar(u.getAvatar());
            }
            vo.setHelpfuled(currentUserId != null && helpfuledIds.contains(a.getId()));
            vo.setCanEdit(currentUserId != null && currentUserId.equals(a.getUserId()));
            vo.setCanDelete(currentUserId != null && currentUserId.equals(a.getUserId()));
            // canAccept: question owner or admin
            Long qOwner = questionOwnerMap.get(a.getQuestionId());
            vo.setCanAccept(currentUserId != null && currentUserId.equals(qOwner));
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }
}