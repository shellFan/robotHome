package com.robot.home.robot.vo;

import com.robot.home.robot.entity.RobotParamDef;
import lombok.Data;

/**
 * 参数定义 + 对应取值
 */
@Data
public class RobotParamDefVO {

    private RobotParamDef def;
    private String value;
}
