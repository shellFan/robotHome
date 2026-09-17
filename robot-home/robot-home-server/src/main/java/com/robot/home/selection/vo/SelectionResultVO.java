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
    /** 关键参数摘要 */
    private List<ParamItem> keyParams;

    @Data
    public static class ParamItem {
        private String label;
        private String value;
    }
}