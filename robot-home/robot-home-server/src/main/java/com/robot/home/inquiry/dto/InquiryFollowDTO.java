package com.robot.home.inquiry.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * 询价跟进 DTO
 */
@Getter
@Setter
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

    @Override
    public String toString() {
        return "InquiryFollowDTO{inquiryId=" + inquiryId + ", followType=" + followType + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InquiryFollowDTO that = (InquiryFollowDTO) o;
        return Objects.equals(inquiryId, that.inquiryId);
    }

    @Override
    public int hashCode() { return Objects.hash(inquiryId); }
}