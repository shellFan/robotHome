package com.robot.home.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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
    private String rankType;

    /** 半衰期（天） */
    private Integer halfLifeDays;

    /** 最低衰减系数（0~1），防止老产品热度归零 */
    private BigDecimal minDecayFactor;

    /** 描述 */
    private String description;
}