package com.robot.home.robot.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 系列新增/编辑请求体
 */
@Data
public class RobotSeriesDTO {

    private Long id;

    @NotNull(message = "品牌不能为空")
    private Long brandId;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "系列名称不能为空")
    @Size(max = 64, message = "名称不能超过 64 字")
    private String name;

    private Integer sort;
    private Integer status;
}