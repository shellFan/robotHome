package com.robot.home.robot.dto;

import lombok.Data;

/**
 * 参数分组新增/编辑请求体
 */
@Data
public class RobotParamGroupDTO {

    private Long id;
    private Long templateId;
    private String name;
    private Integer sort;
}
