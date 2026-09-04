package com.robot.home.robot.dto;

import lombok.Data;

/**
 * 系列新增/编辑请求体
 */
@Data
public class RobotSeriesDTO {

    private Long id;
    private Long brandId;
    private Long categoryId;
    private String name;
    private Integer sort;
    private Integer status;
}
