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
}
