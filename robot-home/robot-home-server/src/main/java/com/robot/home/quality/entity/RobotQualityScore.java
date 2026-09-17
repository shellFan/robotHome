package com.robot.home.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机器人数据质量评分
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_quality_score")
public class RobotQualityScore extends BaseEntity {

    private Long robotId;
    private Integer totalScore;
    private Integer basicInfoScore;
    private Integer paramScore;
    private Integer imageScore;
    private Integer videoScore;
    private Integer docScore;
    private Integer priceScore;
    private Integer specScore;
    private Integer contactScore;
    private Integer brandScore;
    private Integer categoryScore;
    private Integer descScore;
    private Integer qaScore;
    private Integer reviewScore;
}