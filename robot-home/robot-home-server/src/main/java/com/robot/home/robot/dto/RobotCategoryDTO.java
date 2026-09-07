package com.robot.home.robot.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 分类新增/编辑请求体
 */
@Data
public class RobotCategoryDTO {

    private Long id;
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "名称不能超过 32 字")
    private String name;

    private String icon;
    private Integer level;
    private Integer sort;
    private Integer status;
}