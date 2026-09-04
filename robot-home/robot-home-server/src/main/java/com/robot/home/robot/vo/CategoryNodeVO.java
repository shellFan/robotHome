package com.robot.home.robot.vo;

import lombok.Data;

import java.util.List;

/**
 * 分类树节点
 */
@Data
public class CategoryNodeVO {

    private Long id;
    private String name;
    private String icon;
    private Integer level;
    private Integer sort;
    private List<CategoryNodeVO> children;
}
