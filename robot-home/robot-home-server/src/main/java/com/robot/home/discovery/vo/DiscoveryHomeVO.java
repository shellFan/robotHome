package com.robot.home.discovery.vo;

import com.robot.home.brand.vo.BrandListVO;
import com.robot.home.community.vo.PostVO;
import com.robot.home.qa.vo.QuestionVO;
import com.robot.home.robot.vo.CategoryNodeVO;
import com.robot.home.robot.vo.RobotListVO;
import lombok.Data;

import java.util.List;

/**
 * 发现页聚合数据 (Discovery 3.0)
 */
@Data
public class DiscoveryHomeVO {

    /** 精选推荐（大图Banner） */
    private List<RobotListVO> featuredRobots;
    /** 热门机器人 */
    private List<RobotListVO> hotRobots;
    /** 近期热门机器人 */
    private List<RobotListVO> trendingRobots;
    /** 新品/近期收录 */
    private List<RobotListVO> newRobots;
    /** 高评分机器人 */
    private List<RobotListVO> topRatedRobots;
    /** 高收藏机器人 */
    private List<RobotListVO> mostFavoritedRobots;
    /** 高讨论机器人 */
    private List<RobotListVO> mostDiscussedRobots;
    /** 热门品牌 */
    private List<BrandListVO> hotBrands;
    /** 分类入口 */
    private List<CategoryNodeVO> categories;
    /** 热门榜单卡片 */
    private List<DiscoveryRankingCardVO> rankingCards;
    /** 热门讨论 */
    private List<PostVO> hotPosts;
    /** 热门问答 */
    private List<QuestionVO> hotQuestions;
    /** 热门话题 */
    private List<TopicVO> hotTopics;
    /** 平台统计摘要 */
    private DiscoveryStatsVO stats;
    /** 个性化推荐（登录用户） */
    private List<RobotListVO> recommendedRobots;
}