package com.robot.home.robot.vo;

import com.robot.home.robot.entity.RobotParamGroup;
import lombok.Data;

import java.util.List;

/**
 * 参数分组 + 其下参数定义列表
 */
@Data
public class RobotParamGroupVO {

    private RobotParamGroup group;
    private List<RobotParamDefVO> defs;
}
