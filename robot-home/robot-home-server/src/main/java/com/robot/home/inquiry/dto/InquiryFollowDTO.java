package com.robot.home.inquiry.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 询价跟进 DTO
 */
@Data
public class InquiryFollowDTO {

    /** 询价ID */
    @NotNull(message = "询价ID不能为空")
    private Long inquiryId;

    /** 跟进类型: CONTACTED/FOLLOWING/CLOSED/INVALID/NOTE */
    @NotBlank(message = "跟进类型不能为空")
    private String followType;

    /** 跟进内容 */
    @NotBlank(message = "跟进内容不能为空")
    private String content;
}