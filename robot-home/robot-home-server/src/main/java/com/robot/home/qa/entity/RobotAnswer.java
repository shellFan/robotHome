package com.robot.home.qa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机器人问答-回答
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_answer")
public class RobotAnswer extends BaseEntity {

    private Long questionId;
    private Long userId;
    private String content;
    private Integer helpfulCount;
    /** 是否被采纳 */
    private Integer accepted;
    /** 状态: 0待审 1正常 2隐藏 3删除 */
    private Integer status;
}