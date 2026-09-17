package com.robot.home.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机器人口碑评价
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_review")
public class RobotReview extends BaseEntity {

    private Long robotId;
    private Long userId;
    /** 总体评分 1-5 */
    private Integer overallScore;
    /** 质量评分 1-5 */
    private Integer qualityScore;
    /** 服务评分 1-5 */
    private Integer serviceScore;
    /** 性价比评分 1-5 */
    private Integer costScore;
    /** 评价内容 */
    private String content;
    /** 图片URL列表，逗号分隔 */
    private String images;
    /** 状态: 0待审核 1已通过 2已拒绝 */
    private Integer status;
    /** 有用投票数 */
    private Integer helpfulCount;
    /** 官方回复 */
    private String replyContent;
    /** 官方回复时间 */
    private java.time.LocalDateTime replyTime;
}