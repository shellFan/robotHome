package com.robot.home.robot.dto;

import com.robot.home.robot.entity.RobotImage;
import com.robot.home.robot.entity.RobotVideo;
import com.robot.home.robot.entity.RobotPrice;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 机器人新增/编辑请求体
 */
@Data
public class RobotDTO {

    private Long id;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long seriesId;
    private Long brandId;

    @NotBlank(message = "机器人名称不能为空")
    @Size(max = 64, message = "名称不能超过 64 字")
    private String name;

    @Size(max = 64, message = "型号不能超过 64 字")
    private String model;

    @Size(max = 128, message = "副标题过长")
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