package com.robot.home.robot.vo;

import lombok.Data;

/**
 * 筛选项中的品牌选项
 */
@Data
public class BrandOptionVO {

    private Long id;
    private String name;
    private String logo;
    private String initial;
    private Integer robotCount;
}
