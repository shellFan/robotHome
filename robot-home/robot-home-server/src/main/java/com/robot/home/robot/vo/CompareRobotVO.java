package com.robot.home.robot.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 对比中的机器人概览
 */
@Data
public class CompareRobotVO {

    private Long id;
    private String name;
    private String model;
    private String coverImage;
    private String brandName;
    private BigDecimal guidePrice;
}
