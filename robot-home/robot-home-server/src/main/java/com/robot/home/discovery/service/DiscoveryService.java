package com.robot.home.discovery.service;

import com.robot.home.discovery.vo.DiscoveryHomeVO;
import com.robot.home.discovery.vo.DiscoveryRankItemVO;
import com.robot.home.robot.vo.RobotListVO;

import java.util.List;

/**
 * 发现页服务
 */
public interface DiscoveryService {

    /**
     * 发现页聚合数据
     * @param position 展示位置: pc/miniapp
     */
    DiscoveryHomeVO home(Long currentUserId, String position);

    /**
     * 热门机器人
     */
    List<RobotListVO> hotRobots(int limit, Long currentUserId);

    /**
     * 近期热门机器人（7天行为热度）
     */
    List<RobotListVO> trendingRobots(int limit, Long currentUserId);

    /**
     * 新品机器人
     */
    List<RobotListVO> newRobots(int limit, Long currentUserId);

    /**
     * 榜单机器人
     */
    List<DiscoveryRankItemVO> rankRobots(String rankType, int limit);
}