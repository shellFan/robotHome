package com.robot.home.robot.dto;

import lombok.Data;

/**
 * 分类新增/编辑请求体
 */
@Data
public class RobotCategoryDTO {

    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer level;
    private Integer sort;
    private Integer status;
}
