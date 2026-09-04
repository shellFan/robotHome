package com.robot.home.comment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.comment.dto.CommentDTO;
import com.robot.home.comment.entity.Comment;
import com.robot.home.comment.mapper.CommentMapper;
import com.robot.home.comment.service.CommentService;
import com.robot.home.comment.vo.CommentUserVO;
import com.robot.home.comment.vo.CommentVO;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.exception.PermissionException;
import com.robot.home.common.service.BizCounter;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.like.service.LikeService;
import com.robot.home.message.entity.Message;
import com.robot.home.message.mapper.MessageMapper;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用评论服务实现
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    /** 允许评论的业务类型白名单 */
    private static final Set<String> ALLOWED_BIZ = new HashSet<>(Arrays.asList(
            "robot", "article", "video", "tutorial", "post"));

    /** 一级评论默认附带的回复条数 */
    private static final int REPLY_PREVIEW_SIZE = 3;

    @Resource
    private UserMapper userMapper;
    @Resource
    private LikeService likeService;
    @Resource
    private BizCounter bizCounter;
    @Resource
    private MessageMapper messageMapper;

    @Override
    public PageResult<CommentVO> list(String bizType, Long bizId, Long currentUserId, Integer pageNum, Integer pageSize) {
        checkBizType(bizType);
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Comment> page = new Page<>(pn, ps);
        IPage<Comment> result = page(page, Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getBizType, bizType)
                .eq(Comment::getBizId, bizId)
                .and(w -> w.eq(Comment::getParentId, 0L).or().isNull(Comment::getParentId))
                .orderByDesc(Comment::getCreateTime));

        List<Comment> tops = result.getRecords();
        List<CommentVO> vos = new ArrayList<>();
        if (!tops.isEmpty()) {
            List<Long> topIds = tops.stream().map(Comment::getId).collect(Collectors.toList());
            // 一次性取出所有一级评论的预览回复
            List<Comment> replies = list(Wrappers.<Comment>lambdaQuery()
                    .in(Comment::getParentId, topIds)
                    .orderByAsc(Comment::getCreateTime));
            Map<Long, List<Comment>> replyMap = new HashMap<>();
            for (Comment r : replies) {
                replyMap.computeIfAbsent(r.getParentId(), k -> new ArrayList<>()).add(r);
            }
            for (Comment c : tops) {
                CommentVO vo = toVO(c, currentUserId);
                List<Comment> mine = replyMap.getOrDefault(c.getId(), Collections.emptyList());
                List<CommentVO> preview = mine.stream()
                        .limit(REPLY_PREVIEW_SIZE)
                        .map(r -> toVO(r, currentUserId))
                        .collect(Collectors.toList());
                vo.setReplies(preview);
                vos.add(vo);
            }
        }
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public PageResult<CommentVO> replies(Long parentId, Long currentUserId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Comment> page = new Page<>(pn, ps);
        IPage<Comment> result = page(page, Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getParentId, parentId)
                .orderByAsc(Comment::getCreateTime));
        List<CommentVO> vos = result.getRecords().stream()
                .map(c -> toVO(c, currentUserId))
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long userId, CommentDTO dto) {
        checkBizType(dto.getBizType());
        String content = XssUtils.clean(StrUtil.trim(dto.getContent()));
        if (StrUtil.isBlank(content)) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > 1000) {
            throw new BusinessException("评论内容不能超过 1000 字");
        }

        Comment comment = new Comment();
        comment.setBizType(dto.getBizType());
        comment.setBizId(dto.getBizId());
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        comment.setReplyTo(dto.getReplyTo());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);
        save(comment);

        // 业务对象评论数 +1
        bizCounter.incr(dto.getBizType(), dto.getBizId(), BizCounter.Field.COMMENT);

        // 回复：父评论回复数 +1，并给被回复人发消息
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            Comment parent = getById(comment.getParentId());
            if (parent == null) {
                throw new BusinessException("被回复的评论不存在");
            }
            notifyReply(parent, comment);
        }
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long commentId, boolean isAdmin) {
        Comment comment = getById(commentId);
        if (comment == null || comment.getDeleted() != null && comment.getDeleted() == 1) {
            throw new BusinessException("评论不存在或已删除");
        }
        if (!isAdmin && !comment.getUserId().equals(userId)) {
            throw new PermissionException("只能删除自己的评论");
        }
        Comment del = new Comment();
        del.setId(commentId);
        del.setDeleted(1);
        updateById(del);
        bizCounter.decr(comment.getBizType(), comment.getBizId(), BizCounter.Field.COMMENT);
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            update(null, Wrappers.<Comment>lambdaUpdate()
                    .eq(Comment::getId, comment.getParentId())
                    .setSql("reply_count = GREATEST(reply_count - 1, 0)"));
        }
    }

    @Override
    public boolean like(Long userId, Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        return likeService.toggle(userId, "comment", commentId);
    }

    @Override
    public PageResult<CommentVO> myComments(Long userId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Comment> page = new Page<>(pn, ps);
        IPage<Comment> result = page(page, Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getCreateTime));
        List<CommentVO> vos = result.getRecords().stream()
                .map(c -> toVO(c, userId))
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public long count(String bizType, Long bizId) {
        return count(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getBizType, bizType)
                .eq(Comment::getBizId, bizId));
    }

    private void checkBizType(String bizType) {
        if (!ALLOWED_BIZ.contains(bizType)) {
            throw new BusinessException("不支持评论的业务类型: " + bizType);
        }
    }

    private void notifyReply(Comment parent, Comment reply) {
        // 回复自己的评论不通知
        if (parent.getUserId() != null && parent.getUserId().equals(reply.getUserId())) {
            return;
        }
        update(null, Wrappers.<Comment>lambdaUpdate()
                .eq(Comment::getId, parent.getId())
                .setSql("reply_count = reply_count + 1"));

        User from = userMapper.selectById(reply.getUserId());
        Message message = new Message();
        message.setUserId(parent.getUserId());
        message.setType("reply");
        message.setTitle("收到一条回复");
        String nickname = from == null ? "某位用户" : (StrUtil.isNotBlank(from.getNickname()) ? from.getNickname() : from.getUsername());
        message.setContent(nickname + " 回复了你：" + excerpt(reply.getContent()));
        message.setRelatedId(parent.getBizId());
        message.setIsRead(0);
        messageMapper.insert(message);
    }

    private CommentVO toVO(Comment c, Long currentUserId) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setBizType(c.getBizType());
        vo.setBizId(c.getBizId());
        vo.setContent(c.getContent());
        vo.setParentId(c.getParentId());
        vo.setLikeCount(c.getLikeCount());
        vo.setReplyCount(c.getReplyCount());
        vo.setCreateTime(c.getCreateTime());

        User user = userMapper.selectById(c.getUserId());
        if (user != null) {
            CommentUserVO u = new CommentUserVO();
            u.setId(user.getId());
            u.setNickname(StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername());
            u.setAvatar(user.getAvatar());
            vo.setUser(u);
        }
        if (c.getReplyTo() != null && c.getReplyTo() > 0) {
            User to = userMapper.selectById(c.getReplyTo());
            if (to != null) {
                vo.setReplyToNickname(StrUtil.isNotBlank(to.getNickname()) ? to.getNickname() : to.getUsername());
            }
        }
        if (currentUserId != null) {
            vo.setLiked(likeService.check(currentUserId, "comment", c.getId()));
            vo.setCanDelete(currentUserId.equals(c.getUserId()));
        } else {
            vo.setLiked(false);
            vo.setCanDelete(false);
        }
        return vo;
    }

    private String excerpt(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > 60 ? content.substring(0, 60) + "..." : content;
    }
}
