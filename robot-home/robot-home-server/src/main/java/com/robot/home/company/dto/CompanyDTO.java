package com.robot.home.company.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 企业新增/编辑请求体
 */
@Data
public class CompanyDTO {

    private Long id;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 64, message = "企业名称不能超过 64 字")
    private String name;

    @Size(max = 64, message = "英文名称过长")
    private String companyNameEn;

    @Size(max = 32, message = "简称过长")
    private String shortName;

    private String logo;

    @Size(max = 500, message = "简介不能超过 500 字")
    private String intro;

    private Integer foundYear;

    @Size(max = 64, message = "地区过长")
    private String region;

    @Size(max = 32, message = "省份过长")
    private String province;

    @Size(max = 32, message = "城市过长")
    private String city;

    @Size(max = 500, message = "经营范围过长")
    private String businessScope;

    @Size(max = 255, message = "网址过长")
    private String website;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String contactEmail;

    @Size(max = 255, message = "地址过长")
    private String address;

    private String tags;
    private String dataSource;
    private String sourceUrl;
    private Integer brandCount;
    private Integer productCount;
    private Long hotScore;
    private Integer sort;
    private Integer status;
}