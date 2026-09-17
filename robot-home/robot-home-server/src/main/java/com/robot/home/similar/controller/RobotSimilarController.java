package com.robot.home.similar.controller;

import com.robot.home.common.Result;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.similar.service.RobotSimilarService;
import com.robot.home.similar.vo.SimilarRobotVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * C端：相似机器人推荐
 */
@RestController
@RequestMapping("/api/robots/{robotId}/similar")
public class RobotSimilarController {

    @Resource
    private RobotSimilarService similarService;

    /**
     * 获取相似机器人列表
     */
    @GetMapping
    @RateLimit(action = "similar", maxRequests = 20, windowSeconds = 60)
    public Result<List<SimilarRobotVO>> listSimilar(@PathVariable Long robotId,
                                                     @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(similarService.listSimilar(robotId, limit));
    }
}