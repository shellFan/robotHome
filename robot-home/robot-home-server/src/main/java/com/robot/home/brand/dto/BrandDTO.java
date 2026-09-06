package com.robot.home.brand.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 品牌新增/编辑请求体
 */
@Data
public class BrandDTO {

    private Long id;

    @NotBlank(message = "品牌名称不能为空")
    @Size(max = 64, message = "品牌名称不能超过 64 字")
    private String name;

    @Size(max = 64, message = "英文名称过长")
    private String brandNameEn;

    @Size(max = 32, message = "简称过长")
    private String shortName;

    private String logo;
    private Long companyId;

    @Size(max = 500, message = "简介不能超过 500 字")
    private String intro;

    private Integer foundYear;

    @Size(max = 64, message = "国家过长")
    private String country;

    @Size(max = 255, message = "网址过长")
    private String website;

    @Size(max = 1, message = "首字母只能 1 字符")
    private String initial;

    private String dataSource;
    private String sourceUrl;
    private Long hotScore;
    private Integer sort;
    private Integer status;
}