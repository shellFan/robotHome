package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 机器人型号（产品本体）
 * 表名：robot
 */
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

    // --- getter/setter ---

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getSeriesId() { return seriesId; }
    public void setSeriesId(Long seriesId) { this.seriesId = seriesId; }

    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public BigDecimal getGuidePrice() { return guidePrice; }
    public void setGuidePrice(BigDecimal guidePrice) { this.guidePrice = guidePrice; }

    public BigDecimal getMarketPrice() { return marketPrice; }
    public void setMarketPrice(BigDecimal marketPrice) { this.marketPrice = marketPrice; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public Integer getVideoCount() { return videoCount; }
    public void setVideoCount(Integer videoCount) { this.videoCount = videoCount; }

    public String getMainParams() { return mainParams; }
    public void setMainParams(String mainParams) { this.mainParams = mainParams; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public BigDecimal getPayload() { return payload; }
    public void setPayload(BigDecimal payload) { this.payload = payload; }

    public BigDecimal getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(BigDecimal maxSpeed) { this.maxSpeed = maxSpeed; }

    public BigDecimal getBatteryLife() { return batteryLife; }
    public void setBatteryLife(BigDecimal batteryLife) { this.batteryLife = batteryLife; }

    public String getOperatingTemp() { return operatingTemp; }
    public void setOperatingTemp(String operatingTemp) { this.operatingTemp = operatingTemp; }

    public String getProtectionLevel() { return protectionLevel; }
    public void setProtectionLevel(String protectionLevel) { this.protectionLevel = protectionLevel; }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoKeywords() { return seoKeywords; }
    public void setSeoKeywords(String seoKeywords) { this.seoKeywords = seoKeywords; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public java.time.LocalDateTime getLastVerifiedTime() { return lastVerifiedTime; }
    public void setLastVerifiedTime(java.time.LocalDateTime lastVerifiedTime) { this.lastVerifiedTime = lastVerifiedTime; }

    public Long getHotScore() { return hotScore; }
    public void setHotScore(Long hotScore) { this.hotScore = hotScore; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }

    public Integer getCompareCount() { return compareCount; }
    public void setCompareCount(Integer compareCount) { this.compareCount = compareCount; }

    public Integer getInquiryCount() { return inquiryCount; }
    public void setInquiryCount(Integer inquiryCount) { this.inquiryCount = inquiryCount; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public Integer getIsExample() { return isExample; }
    public void setIsExample(Integer isExample) { this.isExample = isExample; }
}