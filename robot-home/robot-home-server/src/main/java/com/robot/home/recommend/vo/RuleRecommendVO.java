package com.robot.home.recommend.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Phase9: 规则推荐项（可解释）
 */
@Data
public class RuleRecommendVO {

    private Long robotId;
    private String name;
    private String coverImage;
    private String subtitle;
    private Long brandId;
    private String brandName;
    private BigDecimal guidePrice;
    private BigDecimal score;
    private Integer favoriteCount;
    private Long hotScore;
    /** 推荐原因代码: SAME_CATEGORY/SAME_BRAND/TRENDING/FOLLOWED_ROBOT/PEOPLE_ALSO_VIEWED/POPULAR_ALTERNATIVE */
    private String reasonCode;
    /** 推荐原因描述 */
    private String reasonText;
}