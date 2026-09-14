package com.robot.home.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.TimeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价有用投票
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_review_helpful")
public class RobotReviewHelpful extends TimeEntity {

    private Long reviewId;
    private Long userId;
}