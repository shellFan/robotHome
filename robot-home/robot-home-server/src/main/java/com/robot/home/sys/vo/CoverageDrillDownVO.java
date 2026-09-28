package com.robot.home.sys.vo;

import lombok.Data;

import java.util.List;

/**
 * Coverage下钻统计VO
 * Phase12 P0-5: 按维度(dataSource/brand/category)下钻覆盖率
 */
@Data
public class CoverageDrillDownVO {

    /** 维度名称: dataSource/brand/category */
    private String dimension;

    /** 统计项列表 */
    private List<DimensionItem> items;

    @Data
    public static class DimensionItem {
        /** 维度值(如dataSource名/brand名/category名) */
        private String name;
        /** 该维度下Robot总数 */
        private Long total;
        /** 有封面图的数量 */
        private Long withCoverImage;
        /** 封面图覆盖率(%) */
        private Double coverImageRate;
        /** 有图集的数量 */
        private Long withGallery;
        /** 图集覆盖率(%) */
        private Double galleryRate;
        /** 有参数的数量 */
        private Long withParams;
        /** 参数覆盖率(%) */
        private Double paramRate;
        /** 有来源URL的数量 */
        private Long withSourceUrl;
        /** 来源URL覆盖率(%) */
        private Double sourceUrlRate;
    }
}