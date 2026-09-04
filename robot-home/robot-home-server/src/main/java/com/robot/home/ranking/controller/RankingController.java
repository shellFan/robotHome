package com.robot.home.ranking.controller;

import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.ranking.service.RankingService;
import com.robot.home.ranking.vo.RankingTypeVO;
import com.robot.home.robot.vo.RobotListVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 排行榜
 */
@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    @Resource
    private RankingService rankingService;

    @GetMapping
    public Result<List<RobotListVO>> rank(@RequestParam(defaultValue = "hot") String type,
                                          @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(rankingService.rank(type, limit, SecurityUtils.currentUserId()));
    }

    @GetMapping("/types")
    public Result<List<RankingTypeVO>> types() {
        return Result.success(rankingService.types());
    }
}
