package com.robot.home.similar.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 相似机器人推荐视图
 */
@Data
public class SimilarRobotVO {
    private Long robotId;
    private String robotName;
    private String imageUrl;
    private BigDecimal price;
    private String categoryName;
    private String brandName;
    /** 总相似分 */
    private BigDecimal totalScore;
    /** 推荐理由 */
    private String reason;
    /** 匹配维度分数明细 */
    private List<ScoreDetail> scoreDetails;

    @Data
    public static class ScoreDetail {
        private String dimension;
        private BigDecimal score;
    }
}