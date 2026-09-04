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
