package com.robot.home.recommend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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
    /** Phase11: 匹配条件明细（可解释推荐） */
    private List<MatchedCondition> matchedConditions;

    /** Phase11: 单个匹配条件 */
    @Data
    public static class MatchedCondition {
        private String code;
        private String text;
        private int score;
    }
}