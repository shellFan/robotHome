package com.robot.home.inquiry.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 询价提交参数
 */
@Data
public class InquiryDTO {

    private Long robotId;

    @NotBlank(message = "请填写姓名")
    @Size(max = 32, message = "姓名过长")
    private String name;

    @NotBlank(message = "请填写手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 64, message = "地区过长")
    private String region;

    /** 1个人 2企业 */
    private Integer customerType;

    @Size(max = 128, message = "企业名称过长")
    private String companyName;

    @NotNull(message = "请填写采购数量")
    private Integer quantity;

    @Size(max = 64, message = "预算描述过长")
    private String budget;

    @Size(max = 500, message = "备注不能超过 500 字")
    private String remark;
}
