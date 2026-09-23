package com.robot.home.robot.vo;

import lombok.Data;

import java.util.List;

/**
 * 对比参数分组
 */
@Data
public class CompareGroupVO {

    private String groupName;
    private List<CompareRowVO> rows;
    /** Phase11: 该分组差异参数行数 */
    private int diffCount;
    /** Phase11: 该分组总参数行数 */
    private int totalCount;
}
