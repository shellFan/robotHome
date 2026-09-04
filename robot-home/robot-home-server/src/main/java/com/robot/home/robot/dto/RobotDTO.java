package com.robot.home.robot.dto;

import com.robot.home.robot.entity.RobotImage;
import com.robot.home.robot.entity.RobotVideo;
import com.robot.home.robot.entity.RobotPrice;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 机器人新增/编辑请求体
 */
@Data
public class RobotDTO {

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
    private BigDecimal score;
    private Integer isExample;

    /** 可选：创建时一并提交图片 */
    private List<RobotImage> imagesList;
    /** 可选：创建时一并提交视频 */
    private List<RobotVideo> videos;
    /** 可选：创建时一并提交价格 */
    private List<RobotPrice> prices;
}
