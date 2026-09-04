package com.robot.home.robot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 机器人列表项（列表 / 搜索 / 排行榜通用）
 */
@Data
public class RobotListVO {

    private Long id;
    private String name;
    private String model;
    private String subtitle;
    private Long brandId;
    private String brandName;
    private String brandLogo;
    private Long categoryId;
    private String categoryName;
    private String coverImage;
    private BigDecimal guidePrice;
    private BigDecimal marketPrice;
    private String mainParams;
    private Integer status;
    private LocalDate releaseDate;
    private Long hotScore;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer compareCount;
    private Integer commentCount;
    private Integer inquiryCount;
    private BigDecimal score;
    /** 当前登录用户是否已收藏 */
    private Boolean favorited;
    /** 标签（场景/开发/AI） */
    private List<String> tags;
}
