package com.robot.home.feed.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.feed.service.FeedService;
import com.robot.home.feed.vo.FeedItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Phase9: 关注动态Feed
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    @Resource
    private FeedService feedService;

    /**
     * 我的关注动态
     */
    @GetMapping("/mine")
    @RateLimit(action = "feed_mine", windowSeconds = 60, maxRequests = 30, dimension = "IP_USER")
    public Result<PageResult<FeedItemVO>> myFeed(
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(feedService.myFeed(SecurityUtils.requireUserId(), lastId, pageSize));
    }
}