package com.robot.home.robot.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 同品牌相关机器人简项
 */
@Data
public class RelatedRobotVO {

    private Long id;
    private String name;
    private String model;
    private String coverImage;
    private BigDecimal guidePrice;
    private String subtitle;
    /** 重量(kg) */
    private BigDecimal weight;
    /** 负载(kg) */
    private BigDecimal payload;
}