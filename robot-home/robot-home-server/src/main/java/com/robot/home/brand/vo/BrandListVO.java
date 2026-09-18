package com.robot.home.brand.vo;

import lombok.Data;

/**
 * 品牌列表项
 */
@Data
public class BrandListVO {

    private Long id;
    private String name;
    private String brandNameEn;
    private String shortName;
    private String logo;
    private String initial;
    private String intro;
    private String country;
    private Integer foundYear;
    private String website;
    private Long companyId;
    private String companyName;
    private Integer robotCount;
    private Long hotScore;
    private String dataSource;
    /** Phase9: 品牌关注数 */
    private Integer followCount;
    /** Phase9: 品牌文章数 */
    private Integer articleCount;
    /** Phase9: 品牌评测数 */
    private Integer reviewCount;
}
