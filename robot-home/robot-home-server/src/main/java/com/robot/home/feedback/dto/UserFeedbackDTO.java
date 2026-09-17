package com.robot.home.feedback.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户反馈提交 DTO
 */
@Data
public class UserFeedbackDTO {

    /** 反馈类型: bug/feature/improvement/other */
    @NotBlank(message = "反馈类型不能为空")
    private String feedbackType;

    /** 反馈内容 */
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容不能超过2000字")
    private String content;

    /** 联系方式（可选） */
    private String contact;

    /** 页面URL */
    private String pageUrl;
}