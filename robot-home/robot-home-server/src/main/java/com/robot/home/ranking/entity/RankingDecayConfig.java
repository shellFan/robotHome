package com.robot.home.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 排行榜时间衰减配置
 * 对应表: ranking_decay_config
 * <p>
 * 衰减公式: score × 2^(-elapsedDays / halfLifeDays)
 * 最低衰减系数: minDecayFactor（防止老产品热度归零）
 */
@Data
@TableName("ranking_decay_config")
public class RankingDecayConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 排行榜类型: hot/humanoid/quadruped/service/industrial/family/dev */
    @NotBlank(message = "排行榜类型不能为空")
    private String rankType;

    /** 半衰期（天） */
    @NotNull(message = "半衰期不能为空")
    @Min(value = 1, message = "半衰期至少1天")
    private Integer halfLifeDays;

    /** 最低衰减系数（0~1），防止老产品热度归零 */
    @DecimalMin(value = "0.0", message = "最低衰减系数不能小于0")
    @DecimalMax(value = "1.0", message = "最低衰减系数不能大于1")
    private BigDecimal minDecayFactor;

    /** 描述 */
    private String description;
}