package com.robot.home.robot.dto;

import lombok.Data;

/**
 * 参数定义新增/编辑请求体
 */
@Data
public class RobotParamDefDTO {

    private Long id;
    private Long groupId;
    private String name;
    private String unit;
    private String type;
    private String options;
    private Integer sort;
    private Integer isCompare;
    private Integer isShow;
}
