package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 机器人型号（产品本体）
 * 表名：robot
 */
@Data
@TableName("robot")
public class Robot extends BaseEntity {

    private Long categoryId;
    private Long seriesId;
    private Long brandId;
    private String name;
    private String model;
    private String subtitle;
    private BigDecimal guidePrice;
    private BigDecimal marketPrice;
    private Integer status;
    private LocalDate releaseDate;
    private String coverImage;
    private String images;
    private Integer videoCount;
    private String mainParams;
    private Long hotScore;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer compareCount;
    private Integer inquiryCount;
    private Integer commentCount;
    private BigDecimal score;
    private Integer isExample;
}
