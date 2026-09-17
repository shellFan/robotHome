package com.robot.home.qa.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 提问参数
 */
@Data
public class QuestionDTO {

    private Long robotId;

    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @Size(max = 5000, message = "内容不能超过5000字")
    private String content;
}