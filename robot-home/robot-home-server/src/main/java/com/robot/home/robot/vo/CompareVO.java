package com.robot.home.robot.vo;

import lombok.Data;

import java.util.List;

/**
 * 参数对比结果：分组 -> 参数行 -> 各机器人取值
 */
@Data
public class CompareVO {

    /** 参与对比的机器人（最多 4 台） */
    private List<CompareRobotVO> robots;
    /** 参数分组（含每行取值） */
    private List<CompareGroupVO> groups;
}
