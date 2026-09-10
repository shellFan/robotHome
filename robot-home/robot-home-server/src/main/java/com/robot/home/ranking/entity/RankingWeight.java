package com.robot.home.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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
    private String eventType;

    /** 权重值 */
    private Integer weight;

    /** 描述 */
    private String description;
}