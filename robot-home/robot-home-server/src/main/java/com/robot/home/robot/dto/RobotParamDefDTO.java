package com.robot.home.robot.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 参数定义新增/编辑请求体
 */
@Data
public class RobotParamDefDTO {

    private Long id;

    @NotNull(message = "分组不能为空")
    private Long groupId;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 64, message = "名称不能超过 64 字")
    private String name;

    @Size(max = 16, message = "单位过长")
    private String unit;

    private String type;
    private String options;
    private Integer sort;
    private Integer isCompare;
    private Integer isShow;
}