package com.robot.home.community.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.comment.entity.Comment;
import com.robot.home.comment.mapper.CommentMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.community.entity.CommunityCircle;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityCircleMapper;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.security.RequirePermission;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 后台社区管理：圈子 / 帖子审核 / 评论
 */
@RestController
@RequestMapping("/api/admin/community")
public class AdminCommunityController {

    @Resource
    private CommunityCircleMapper circleMapper;
    @Resource
    private CommunityPostMapper postMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private UserMapper userMapper;

    // ---------------- 圈子 ----------------

    @GetMapping("/circles")
    @RequirePermission("community:circle")
    public Result<List<CommunityCircle>> circles() {
        return Result.success(circleMapper.selectList(Wrappers.<CommunityCircle>lambdaQuery()
                .orderByAsc(CommunityCircle::getSort)));
    }

    @PostMapping("/circles")
    @RequirePermission("community:circle")
    public Result<Long> saveCircle(@RequestBody CommunityCircle circle) {
        if (StrUtil.isBlank(circle.getName())) {
            throw new BusinessException("圈子名称不能为空");
        }
        if (circle.getId() == null) {
            if (circle.getStatus() == null) {
                circle.setStatus(1);
            }
            circleMapper.insert(circle);
        } else {
            circleMapper.updateById(circle);
        }
        return Result.success(circle.getId());
    }

    @DeleteMapping("/circles/{id}")
    @RequirePermission("community:circle")
    public Result<Void> deleteCircle(@PathVariable Long id) {
        long count = postMapper.selectCount(Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getCircleId, id));
        if (count > 0) {
            throw new BusinessException("该圈子下仍有 " + count + " 个帖子，不能删除");
        }
        circleMapper.deleteById(id);
        return Result.success();
    }

    // ---------------- 帖子 ----------------

    @GetMapping("/posts")
    @RequirePermission("community:post")
    public Result<PageResult<CommunityPost>> posts(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long circleId,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<CommunityPost> page = new Page<>(pn, ps);
        IPage<CommunityPost> result = postMapper.selectPage(page, Wrappers.<CommunityPost>lambdaQuery()
                .likeRight(StrUtil.isNotBlank(keyword), CommunityPost::getTitle, keyword)
                .eq(circleId != null, CommunityPost::getCircleId, circleId)
                .eq(status != null, CommunityPost::getStatus, status)
                .orderByDesc(CommunityPost::getCreateTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    /**
     * 帖子审核：0待审 1正常 2下架
     */
    @PostMapping("/posts/{id}/status")
    @RequirePermission("community:post:audit")
    public Result<Void> auditPost(@PathVariable Long id, @RequestParam Integer status) {
        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        CommunityPost update = new CommunityPost();
        update.setId(id);
        update.setStatus(status);
        postMapper.updateById(update);
        return Result.success();
    }

    @DeleteMapping("/posts/{id}")
    @RequirePermission("community:post")
    public Result<Void> deletePost(@PathVariable Long id) {
        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        postMapper.deleteById(id);
        commentMapper.delete(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getBizType, "post").eq(Comment::getBizId, id));
        if (post.getUserId() != null) {
            User user = userMapper.selectById(post.getUserId());
            if (user != null && user.getPostCount() != null && user.getPostCount() > 0) {
                User update = new User();
                update.setId(user.getId());
                update.setPostCount(user.getPostCount() - 1);
                userMapper.updateById(update);
            }
        }
        return Result.success();
    }

    // ---------------- 评论 ----------------

    @GetMapping("/comments")
    @RequirePermission("community:comment")
    public Result<PageResult<Comment>> comments(@RequestParam(required = false) String bizType,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Comment> page = new Page<>(pn, ps);
        IPage<Comment> result = commentMapper.selectPage(page, Wrappers.<Comment>lambdaQuery()
                .eq(StrUtil.isNotBlank(bizType), Comment::getBizType, bizType)
                .likeRight(StrUtil.isNotBlank(keyword), Comment::getContent, keyword)
                .orderByDesc(Comment::getCreateTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @DeleteMapping("/comments/{id}")
    @RequirePermission("community:comment:delete")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        commentMapper.deleteById(id);
        return Result.success();
    }
}
