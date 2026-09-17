package com.robot.home.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机器人数据质量问题
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_quality_issue")
public class RobotQualityIssue extends BaseEntity {

    private Long robotId;
    /** MISSING_BASIC_INFO/MISSING_IMAGES/MISSING_VIDEO/MISSING_PRICE/MISSING_PARAMS/MISSING_CONTACT/MISSING_CATEGORY/LOW_QUALITY_DESC */
    private String issueType;
    private String description;
    /** 严重度: 1高 2中 3低 */
    private Integer severity;
    /** 0未处理 1已处理 2忽略 */
    private Integer status;
}