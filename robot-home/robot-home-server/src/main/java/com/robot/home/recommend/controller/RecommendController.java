package com.robot.home.recommend.controller;

import com.robot.home.common.Result;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.recommend.service.RecommendService;
import com.robot.home.recommend.service.RuleRecommendService;
import com.robot.home.recommend.vo.RecommendItemVO;
import com.robot.home.recommend.vo.RuleRecommendVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 推荐位
 */
@RestController
@RequestMapping("/api/recommends")
public class RecommendController {

    @Resource
    private RecommendService recommendService;
    @Resource
    private RuleRecommendService ruleRecommendService;

    @GetMapping("/{code}")
    @RateLimit(action = "recommend_items", windowSeconds = 60, maxRequests = 30, dimension = "IP")
    public Result<List<RecommendItemVO>> items(@PathVariable String code) {
        return Result.success(recommendService.items(code));
    }

    /**
     * Phase9: 机器人相关推荐（规则型、可解释）
     */
    @GetMapping("/robots/{robotId}/related")
    @RateLimit(action = "recommend_related", windowSeconds = 60, maxRequests = 30, dimension = "IP")
    public Result<List<RuleRecommendVO>> relatedRobots(
            @PathVariable Long robotId,
            @RequestParam(defaultValue = "6") Integer limit) {
        return Result.success(ruleRecommendService.relatedRobots(robotId, SecurityUtils.currentUserId(), limit));
    }
}
