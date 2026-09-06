package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发布到主站 robot 表的实体（采集器侧映射）
 */
@Data
@TableName("robot")
public class PublishRobot {

    private Long id;
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
    private String mainParams;
    private String dataSource;
    private String sourceUrl;
    private String sourceName;
    private BigDecimal score;
    private Integer isExample;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}