package com.robot.home.company.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

/**
 * 企业
 * 表名：company
 */
@Data
@TableName("company")
public class Company extends BaseEntity {

    private String name;
    /** 公司英文名 */
    private String companyNameEn;
    /** 简称 */
    private String shortName;
    private String logo;
    private String intro;
    /** 数据来源: DEMO/OFFICIAL/CRAWLER/MANUAL */
    private String dataSource;
    /** 经营范围 */
    private String businessScope;
    /** 来源URL */
    private String sourceUrl;
    /** 最后验证时间 */
    private java.time.LocalDateTime lastVerifiedTime;
    private Integer foundYear;
    private String region;
    /** 省份 */
    private String province;
    /** 城市 */
    private String city;
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
