package com.robot.home.qa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机器人问答-问题
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_question")
public class RobotQuestion extends BaseEntity {

    private Long userId;
    private Long robotId;
    private String title;
    private String content;
    /** 状态: 0待审 1正常 2隐藏 3删除 */
    private Integer status;
    private Integer answerCount;
    private Integer followCount;
    private Integer viewCount;
    private Integer hasAccepted;
}