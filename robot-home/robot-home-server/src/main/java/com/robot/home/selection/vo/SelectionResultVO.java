package com.robot.home.selection.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 选型结果
 */
@Data
public class SelectionResultVO {

    private Long robotId;
    private String robotName;
    private String coverImage;
    private BigDecimal guidePrice;
    private String categoryName;
    private String brandName;
    private Integer matchScore;
    private String matchReason;
    /** Phase11: 匹配置信度(0-100), 基于匹配条件数/总条件数 */
    private Integer confidence;
    /** 关键参数摘要 */
    private List<ParamItem> keyParams;
    /** Phase11: 匹配条件明细（可解释推荐） */
    private List<MatchedCondition> matchedConditions;

    @Data
    public static class ParamItem {
        private String label;
        private String value;
    }

    /** Phase11: 单个匹配条件 */
    @Data
    public static class MatchedCondition {
        /** 条件代码: CATEGORY_MATCH/BUDGET_MATCH/BRAND_MATCH/USAGE_MATCH/FILTER_MATCH */
        private String code;
        /** 条件描述 */
        private String text;
        /** 该条件得分 */
        private int score;
    }
}