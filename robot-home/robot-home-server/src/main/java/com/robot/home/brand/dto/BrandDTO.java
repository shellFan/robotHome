package com.robot.home.brand.dto;

import lombok.Data;

/**
 * 品牌新增/编辑请求体
 */
@Data
public class BrandDTO {

    private Long id;
    private String name;
    private String logo;
    private Long companyId;
    private String intro;
    private Integer foundYear;
    private String country;
    private String website;
    private String initial;
    private Long hotScore;
    private Integer sort;
    private Integer status;
}
