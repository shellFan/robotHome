package com.robot.home.company.dto;

import lombok.Data;

/**
 * 企业新增/编辑请求体
 */
@Data
public class CompanyDTO {

    private Long id;
    private String name;
    private String logo;
    private String intro;
    private Integer foundYear;
    private String region;
    private String website;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String tags;
    private Integer brandCount;
    private Integer productCount;
    private Long hotScore;
    private Integer sort;
    private Integer status;
}
