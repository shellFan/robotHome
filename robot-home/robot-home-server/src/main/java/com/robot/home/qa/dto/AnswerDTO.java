package com.robot.home.qa.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 回答参数
 */
@Data
public class AnswerDTO {

    @NotBlank(message = "回答内容不能为空")
    @Size(max = 5000, message = "回答不能超过5000字")
    private String content;
}