package com.robot.home.robot.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格区间筛选项
 */
@Data
public class PriceRangeVO {

    private String label;
    private BigDecimal min;
    private BigDecimal max;

    public PriceRangeVO() {
    }

    public PriceRangeVO(String label, BigDecimal min, BigDecimal max) {
        this.label = label;
        this.min = min;
        this.max = max;
    }
}
