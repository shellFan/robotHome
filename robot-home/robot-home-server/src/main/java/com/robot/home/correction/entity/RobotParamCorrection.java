package com.robot.home.correction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机器人参数纠错
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_param_correction")
public class RobotParamCorrection extends BaseEntity {

    private Long robotId;
    private Long userId;
    /** 参数定义ID */
    private Long defId;
    /** 参数名称（冗余，便于展示） */
    private String paramName;
    /** 原值 */
    private String oldValue;
    /** 新值 */
    private String newValue;
    /** 纠错理由 */
    private String reason;
    /** 状态: 0待审核 1已采纳 2已拒绝 */
    private Integer status;
    /** 审核人ID */
    private Long reviewerId;
    /** 审核备注 */
    private String reviewNote;
    /** 审核时间 */
    private java.time.LocalDateTime reviewTime;
}