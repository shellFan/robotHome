package com.robot.home.company.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase9: 企业主页聚合VO
 * 包含企业详情+品牌+机器人+新闻+讨论
 */
@Data
public class CompanyPageVO {

    /** 企业基本信息 */
    private CompanyDetailVO company;

    /** 企业关注数 */
    private Integer followCount;

    /** 当前用户是否已关注 */
    private Boolean followed;

    /** 品牌列表（含简单信息） */
    private List<BrandSimpleVO> brands;

    /** 热门机器人 */
    private List<RobotSimpleVO> hotRobots;

    /** 相关文章 */
    private List<ArticleSimpleVO> articles;

    /** 社区讨论 */
    private List<PostSimpleVO> posts;

    /** 机器人总数 */
    private Long robotCount;

    /** 品牌总数 */
    private Long brandCount;

    /** 文章总数 */
    private Long articleCount;

    // ========== 内嵌简单VO ==========

    @Data
    public static class BrandSimpleVO {
        private Long id;
        private String name;
        private String logo;
        private String country;
        private Integer robotCount;
    }

    @Data
    public static class RobotSimpleVO {
        private Long id;
        private String name;
        private String coverImage;
        private String subtitle;
        private java.math.BigDecimal guidePrice;
        private java.math.BigDecimal score;
        private Long brandId;
        private String brandName;
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
    public static class PostSimpleVO {
        private Long id;
        private String title;
        private Integer likeCount;
        private Integer commentCount;
        private LocalDateTime createTime;
    }
}