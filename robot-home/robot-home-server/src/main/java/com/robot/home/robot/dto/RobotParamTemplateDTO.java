package com.robot.home.robot.dto;

import lombok.Data;

/**
 * 参数模板新增/编辑请求体
 */
@Data
public class RobotParamTemplateDTO {

    private Long id;
    private Long categoryId;
    private String name;
    private Integer sort;
    private Integer status;
}
