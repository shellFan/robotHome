package com.robot.home.robot.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 机器人库筛选查询条件
 */
@Data
public class RobotQuery {

    /** 分类 id（传入一级分类时自动包含其子分类） */
    private Long categoryId;
    /** 已展开的分类 id 集合（含子分类） */
    private List<Long> categoryIds;
    private Long brandId;
    private Long seriesId;
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    /** 使用场景（robot_tag.tag_type = scene） */
    private List<String> scenes;
    /** 开发能力（dev） */
    private List<String> devs;
    /** AI 能力（ai） */
    private List<String> ais;
    /** 发布时间范围 */
    private String releaseStart;
    private String releaseEnd;
    /** 排序：comprehensive / hot / price_asc / price_desc / new / score */
    private String sort;
    private Integer pageNum;
    private Integer pageSize;
}
