package com.robot.home.article.vo;

import lombok.Data;

/**
 * 通用分类（带数量），资讯/视频/教程共用
 */
@Data
public class CategoryCountVO {

    private Long id;
    private String name;
    private Integer sort;
    private Long count;

    public CategoryCountVO() {
    }

    public CategoryCountVO(Long id, String name, Integer sort, Long count) {
        this.id = id;
        this.name = name;
        this.sort = sort;
        this.count = count;
    }
}
