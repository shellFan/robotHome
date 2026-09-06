package com.robot.home.brand.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

/**
 * 品牌
 * 表名：brand
 */
@Data
@TableName("brand")
public class Brand extends BaseEntity {

    private String name;
    /** 品牌英文名 */
    private String brandNameEn;
    /** 简称 */
    private String shortName;
    private String logo;
    private Long companyId;
    private String intro;
    /** 数据来源: DEMO/OFFICIAL/CRAWLER/MANUAL */
    private String dataSource;
    private Integer foundYear;
    private String country;
    /** 来源URL */
    private String sourceUrl;
    /** 最后验证时间 */
    private java.time.LocalDateTime lastVerifiedTime;
    private String website;
    private String initial;
    /** 机器人数量（冗余，程序维护） */
    private Integer robotCount;
    private Long hotScore;
    private Integer sort;
    private Integer status;
}
