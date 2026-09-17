package com.robot.home.correction.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 提交参数纠错DTO
 */
@Data
public class ParamCorrectionDTO {
    @NotNull(message = "机器人ID不能为空")
    private Long robotId;
    @NotNull(message = "参数定义ID不能为空")
    private Long defId;
    @NotBlank(message = "新值不能为空")
    @Size(max = 500, message = "新值长度不能超过500")
    private String newValue;
    @NotBlank(message = "纠错理由不能为空")
    @Size(min = 5, max = 500, message = "纠错理由长度5-500")
    private String reason;
}