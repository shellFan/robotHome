package com.robot.home.robot.vo;

import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotImage;
import com.robot.home.robot.entity.RobotPrice;
import com.robot.home.robot.entity.RobotVideo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 机器人详情视图对象
 */
@Data
public class RobotDetailVO {

    private Robot robot;
    private String brandName;
    private String brandLogo;
    private Long brandId;
    private String categoryName;
    private String parentCategoryName;
    private String companyName;
    private Long companyId;
    private List<RobotImage> images;
    private List<RobotVideo> videos;
    private List<RobotParamGroupVO> paramGroups;
    private List<RobotPrice> prices;
    private List<String> scenes;
    private List<String> devs;
    private List<String> ais;
    /** 相关资讯 */
    private List<RelatedArticleVO> articles;
    private Boolean favorited;
    private Long favoriteCount;

    // ===== Phase6 产品化增强字段 =====

    /** SEO标题（优先取robot.seoTitle，否则自动生成） */
    private String seoTitle;
    /** SEO关键词 */
    private String seoKeywords;
    /** SEO描述 */
    private String seoDescription;

    /** 核心参数：重量(kg) */
    private BigDecimal weight;
    /** 核心参数：负载(kg) */
    private BigDecimal payload;
    /** 核心参数：最大速度(m/s) */
    private BigDecimal maxSpeed;
    /** 核心参数：续航时间(h) */
    private BigDecimal batteryLife;
    /** 核心参数：工作温度范围 */
    private String operatingTemp;
    /** 核心参数：防护等级 */
    private String protectionLevel;

    /** 同品牌其他机器人 */
    private List<RelatedRobotVO> sameBrandRobots;
    /** 相关视频 */
    private List<RelatedArticleVO> relatedVideos;
}
