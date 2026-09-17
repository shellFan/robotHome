package com.robot.home.review.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 提交口碑评价参数
 */
@Data
public class ReviewDTO {

    @NotNull(message = "机器人ID不能为空")
    private Long robotId;

    @NotNull(message = "总体评分不能为空")
    @Min(value = 1, message = "评分最低1")
    @Max(value = 5, message = "评分最高5")
    private Integer overallScore;

    @NotNull(message = "质量评分不能为空")
    @Min(1) @Max(5)
    private Integer qualityScore;

    @NotNull(message = "服务评分不能为空")
    @Min(1) @Max(5)
    private Integer serviceScore;

    @NotNull(message = "性价比评分不能为空")
    @Min(1) @Max(5)
    private Integer costScore;

    @NotNull(message = "评价内容不能为空")
    @Size(min = 10, max = 2000, message = "评价内容10-2000字")
    private String content;

    /** 图片URL列表，逗号分隔 */
    private String images;
}