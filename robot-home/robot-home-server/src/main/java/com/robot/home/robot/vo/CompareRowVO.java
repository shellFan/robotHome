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
}
