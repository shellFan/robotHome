package com.robot.home.robot.vo;

import lombok.Data;

import java.util.List;

/**
 * 对比参数行：与 robots 顺序一一对应的取值
 */
@Data
public class CompareRowVO {

    private Long defId;
    private String paramName;
    private String unit;
    /** 与 robots 顺序一一对应的取值 */
    private List<String> values;
    /** true 表示各机器人取值存在差异（用于差异高亮） */
    private boolean different;
    /** 对比类型: HIGHER_BETTER/LOWER_BETTER/NEUTRAL/BOOLEAN/TEXT */
    private String comparisonType;
    /** 最优值所在的索引（0-based），-1表示无法判断或无差异 */
    private int bestIndex;
}
