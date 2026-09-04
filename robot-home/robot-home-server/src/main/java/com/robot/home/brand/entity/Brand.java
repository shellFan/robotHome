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
    private String logo;
    private Long companyId;
    private String intro;
    private Integer foundYear;
    private String country;
    private String website;
    private String initial;
    /** 机器人数量（冗余，程序维护） */
    private Integer robotCount;
    private Long hotScore;
    private Integer sort;
    private Integer status;
}
