package com.robot.home.community.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.community.dto.PostDTO;
import com.robot.home.community.service.CommunityService;
import com.robot.home.community.vo.CircleVO;
import com.robot.home.community.vo.PostVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 社区：圈子 / 帖子
 */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Resource
    private CommunityService communityService;

    @GetMapping("/circles")
    public Result<List<CircleVO>> circles() {
        return Result.success(communityService.circles(SecurityUtils.currentUserId()));
    }

    @GetMapping("/posts")
    public Result<PageResult<PostVO>> posts(@RequestParam(required = false) Long circleId,
                                            @RequestParam(required = false) String topic,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "latest") String sort,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(communityService.posts(circleId, topic, keyword, sort,
                SecurityUtils.currentUserId(), pageNum, pageSize));
    }

    @GetMapping("/topics")
    public Result<List<String>> topics(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(communityService.hotTopics(limit));
    }

    @GetMapping("/posts/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        return Result.success(communityService.detail(id, SecurityUtils.currentUserId()));
    }

    /**
     * 发帖
     */
    @PostMapping("/posts")
    public Result<Map<String, Object>> create(@RequestBody @Valid PostDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        Long id = communityService.create(userId, dto);
        return Result.success(java.util.Collections.singletonMap("id", id));
    }

    /**
     * 删帖（本人）
     */
    @DeleteMapping("/posts/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        communityService.delete(userId, id, false);
        return Result.success();
    }

    /**
     * 我的帖子
     */
    @GetMapping("/my-posts")
    public Result<PageResult<PostVO>> myPosts(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(communityService.myPosts(userId, pageNum, pageSize));
    }
}
