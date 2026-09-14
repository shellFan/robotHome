package com.robot.home.similar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 相似机器人评分
 */
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRobotId() { return robotId; }
    public void setRobotId(Long robotId) { this.robotId = robotId; }

    public Long getSimilarRobotId() { return similarRobotId; }
    public void setSimilarRobotId(Long similarRobotId) { this.similarRobotId = similarRobotId; }

    public BigDecimal getCategoryScore() { return categoryScore; }
    public void setCategoryScore(BigDecimal categoryScore) { this.categoryScore = categoryScore; }

    public BigDecimal getPriceScore() { return priceScore; }
    public void setPriceScore(BigDecimal priceScore) { this.priceScore = priceScore; }

    public BigDecimal getBrandScore() { return brandScore; }
    public void setBrandScore(BigDecimal brandScore) { this.brandScore = brandScore; }

    public BigDecimal getParamScore() { return paramScore; }
    public void setParamScore(BigDecimal paramScore) { this.paramScore = paramScore; }

    public BigDecimal getTagScore() { return tagScore; }
    public void setTagScore(BigDecimal tagScore) { this.tagScore = tagScore; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

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