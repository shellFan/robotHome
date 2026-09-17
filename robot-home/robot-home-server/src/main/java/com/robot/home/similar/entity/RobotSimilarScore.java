package com.robot.home.similar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 相似机器人评分
 */
@Getter
@Setter
@TableName("robot_similar_score")
public class RobotSimilarScore {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long robotId;
    private Long similarRobotId;
    private BigDecimal categoryScore;
    private BigDecimal priceScore;
    private BigDecimal brandScore;
    private BigDecimal paramScore;
    private BigDecimal tagScore;
    private BigDecimal totalScore;
    private String reason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Override
    public String toString() {
        return "RobotSimilarScore{id=" + id + ", robotId=" + robotId + ", similarRobotId=" + similarRobotId +
               ", totalScore=" + totalScore + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RobotSimilarScore that = (RobotSimilarScore) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}