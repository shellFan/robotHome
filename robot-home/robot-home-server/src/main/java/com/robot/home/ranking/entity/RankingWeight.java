package com.robot.home.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 排行榜权重配置
 * 对应表: ranking_weight
 */
@Data
@TableName("ranking_weight")
public class RankingWeight {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件类型: VIEW/FAVORITE/COMPARE/INQUIRY/COMMENT/SCORE/NEW_PRODUCT */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /** 权重值 */
    @NotNull(message = "权重值不能为空")
    @Min(value = 0, message = "权重值不能为负")
    @Max(value = 100, message = "权重值不能超过100")
    private Integer weight;

    /** 描述 */
    private String description;
}