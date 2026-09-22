package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 机器人型号（产品本体）
 * 表名：robot
 */
@Getter
@Setter
@TableName("robot")
public class Robot extends BaseEntity {

    private Long categoryId;
    private Long seriesId;
    private Long brandId;
    private String name;
    private String model;
    private String subtitle;
    private BigDecimal guidePrice;
    private BigDecimal marketPrice;
    private Integer status;
    private LocalDate releaseDate;
    private String coverImage;
    private String images;
    private Integer videoCount;
    private String mainParams;
    /** 重量(kg) */
    private BigDecimal weight;
    /** 负载(kg) */
    private BigDecimal payload;
    /** 最大速度(m/s) */
    private BigDecimal maxSpeed;
    /** 续航时间(h) */
    private BigDecimal batteryLife;
    /** 工作温度范围 */
    private String operatingTemp;
    /** 防护等级(IP54等) */
    private String protectionLevel;
    /** SEO标题 */
    private String seoTitle;
    /** SEO关键词 */
    private String seoKeywords;
    /** SEO描述 */
    private String seoDescription;
    /** 图文详情（富文本HTML） */
    private String detail;
    /** 数据来源: DEMO/OFFICIAL/CRAWLER/MANUAL */
    private String dataSource;
    /** 来源URL */
    private String sourceUrl;
    /** 来源名称 */
    private String sourceName;
    /** 最后验证时间 */
    private java.time.LocalDateTime lastVerifiedTime;
    private Long hotScore;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer compareCount;
    private Integer inquiryCount;
    private Integer commentCount;
    private BigDecimal score;
    private Integer isExample;
    /** Phase9: 讨论数 */
    private Integer discussionCount;
    /** Phase9: 提问数 */
    private Integer questionCount;
    /** Phase9: 评测数 */
    private Integer reviewCount;
    /** Phase9: 关注数 */
    private Integer followCount;

    /** Phase10: 信任等级 */
    private String trustLevel;
    /** Phase10: 信任分 */
    private Integer trustScore;
    /** Phase10: 待处理纠错数 */
    private Integer pendingCorrectionCount;

    // --- 别名方法(兼容旧接口) ---
    public BigDecimal getPrice() { return guidePrice; }
    public String getImageUrl() { return coverImage; }
}