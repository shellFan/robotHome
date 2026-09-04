package com.robot.home.robot.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 机器人简要视图对象（用于品牌/企业详情中的产品列表）
 */
@Data
public class RobotSummaryVO {

    private Long id;
    private String name;
    private String model;
    private String coverImage;
    private BigDecimal guidePrice;
    private String brandName;
    private Integer status;
}
