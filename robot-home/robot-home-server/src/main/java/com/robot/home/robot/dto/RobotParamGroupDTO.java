package com.robot.home.robot.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 参数分组新增/编辑请求体
 */
@Data
public class RobotParamGroupDTO {

    private Long id;

    @NotNull(message = "模板不能为空")
    private Long templateId;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 64, message = "名称不能超过 64 字")
    private String name;

    private Integer sort;
}