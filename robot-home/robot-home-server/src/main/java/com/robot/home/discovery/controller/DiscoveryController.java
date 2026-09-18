package com.robot.home.discovery.controller;

import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.discovery.service.DiscoveryService;
import com.robot.home.discovery.vo.DiscoveryHomeVO;
import com.robot.home.discovery.vo.DiscoveryRankItemVO;
import com.robot.home.robot.vo.RobotListVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 发现页
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    @Resource
    private DiscoveryService discoveryService;

    /**
     * 发现页聚合数据
     * @param position 展示位置: pc/miniapp，影响返回数量
     */
    @GetMapping("/home")
    public Result<DiscoveryHomeVO> home(@RequestParam(required = false) String position) {
        return Result.success(discoveryService.home(SecurityUtils.currentUserId(), position));
    }

    /**
     * 热门机器人
     */
    @GetMapping("/hot")
    public Result<List<RobotListVO>> hotRobots(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(discoveryService.hotRobots(limit, SecurityUtils.currentUserId()));
    }

    /**
     * 近期热门（7天行为热度）
     */
    @GetMapping("/trending")
    public Result<List<RobotListVO>> trendingRobots(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(discoveryService.trendingRobots(limit, SecurityUtils.currentUserId()));
    }

    /**
     * 新品机器人
     */
    @GetMapping("/new")
    public Result<List<RobotListVO>> newRobots(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(discoveryService.newRobots(limit, SecurityUtils.currentUserId()));
    }

    /**
     * 榜单机器人
     */
    @GetMapping("/rank")
    public Result<List<DiscoveryRankItemVO>> rankRobots(
            @RequestParam(defaultValue = "hot") String type,
            @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(discoveryService.rankRobots(type, limit));
    }
}