package com.robot.home.comment.controller;

import com.robot.home.comment.dto.CommentDTO;
import com.robot.home.comment.service.CommentService;
import com.robot.home.comment.vo.CommentVO;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

/**
 * 通用评论：robot / article / video / tutorial / post
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Resource
    private CommentService commentService;

    /**
     * 评论列表（一级评论 + 预览回复）
     */
    @GetMapping
    public Result<PageResult<CommentVO>> list(@RequestParam String bizType,
                                              @RequestParam Long bizId,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(commentService.list(bizType, bizId, SecurityUtils.currentUserId(), pageNum, pageSize));
    }

    /**
     * 某条评论的全部回复
     */
    @GetMapping("/{id}/replies")
    public Result<PageResult<CommentVO>> replies(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(commentService.replies(id, SecurityUtils.currentUserId(), pageNum, pageSize));
    }

    @PostMapping
    public Result<Map<String, Object>> add(@RequestBody @Valid CommentDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        Long id = commentService.add(userId, dto);
        return Result.success(Collections.singletonMap("id", id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        commentService.delete(userId, id, false);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> like(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        boolean liked = commentService.like(userId, id);
        return Result.success(Collections.singletonMap("liked", liked));
    }

    @GetMapping("/my")
    public Result<PageResult<CommentVO>> my(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(commentService.myComments(userId, pageNum, pageSize));
    }

    @GetMapping("/count")
    public Result<Map<String, Object>> count(@RequestParam String bizType, @RequestParam Long bizId) {
        return Result.success(Collections.singletonMap("count", commentService.count(bizType, bizId)));
    }
}
