package com.robot.home.robot.vo;

import lombok.Data;

import java.util.List;

/**
 * 机器人库筛选项聚合
 */
@Data
public class RobotFilterVO {

    /** 分类树（一级 + 二级） */
    private List<CategoryNodeVO> categories;
    /** 热门品牌 */
    private List<BrandOptionVO> brands;
    /** 使用场景 */
    private List<String> scenes;
    /** 开发能力 */
    private List<String> devs;
    /** AI 能力 */
    private List<String> ais;
    /** 价格区间（元） */
    private List<PriceRangeVO> priceRanges;
}
