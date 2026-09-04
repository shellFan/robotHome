package com.robot.home.robot.vo;

import com.robot.home.robot.entity.RobotParamTemplate;
import lombok.Data;

import java.util.List;

/**
 * 参数模板详情（含分组与定义）
 */
@Data
public class ParamTemplateDetailVO {

    private RobotParamTemplate template;
    private List<RobotParamGroupVO> groups;
}
