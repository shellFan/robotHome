package com.robot.home.company.vo;

import lombok.Data;

import java.util.List;

/**
 * 企业列表项
 */
@Data
public class CompanyListVO {

    private Long id;
    private String name;
    private String companyNameEn;
    private String shortName;
    private String logo;
    private String intro;
    private Integer foundYear;
    private String region;
    private String province;
    private String city;
    private String website;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private List<String> tags;
    private Integer brandCount;
    private Integer productCount;
    private Long hotScore;
    private String dataSource;
}
