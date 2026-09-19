package com.robot.home.brand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase9: 品牌主页聚合VO
 * 包含品牌详情+旗下机器人+相关新闻+评测+社区讨论+Q&A
 */
@Data
public class BrandPageVO {

    /** 品牌基本信息 */
    private BrandDetailVO brand;

    /** 品牌关注数 */
    private Integer followCount;

    /** 当前用户是否已关注 */
    private Boolean followed;

    /** 热门机器人 */
    private List<RobotSimpleVO> hotRobots;

    /** 新品机器人 */
    private List<RobotSimpleVO> newRobots;

    /** 相关文章 */
    private List<ArticleSimpleVO> articles;

    /** 相关评测 */
    private List<ReviewSimpleVO> reviews;

    /** 社区讨论 */
    private List<PostSimpleVO> posts;

    /** 相关问答 */
    private List<QuestionSimpleVO> questions;

    /** 机器人总数 */
    private Long robotCount;

    /** 文章总数 */
    private Long articleCount;

    /** 评测总数 */
    private Long reviewCount;

    // ========== 内嵌简单VO ==========

    @Data
    public static class RobotSimpleVO {
        private Long id;
        private String name;
        private String coverImage;
        private String subtitle;
        private BigDecimal guidePrice;
        private BigDecimal score;
        private Integer favoriteCount;
    }

    @Data
    public static class ArticleSimpleVO {
        private Long id;
        private String title;
        private String coverImage;
        private Integer viewCount;
        private LocalDateTime publishTime;
    }

    @Data
    public static class ReviewSimpleVO {
        private Long id;
        private String title;
        private BigDecimal score;
        private Integer viewCount;
        private LocalDateTime createTime;
    }

    @Data
    public static class PostSimpleVO {
        private Long id;
        private String title;
        private Integer likeCount;
        private Integer commentCount;
        private LocalDateTime createTime;
    }

    @Data
    public static class QuestionSimpleVO {
        private Long id;
        private String title;
        private Integer answerCount;
        private Integer viewCount;
        private LocalDateTime createTime;
    }
}