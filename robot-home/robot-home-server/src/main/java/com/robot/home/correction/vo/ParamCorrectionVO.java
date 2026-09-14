package com.robot.home.correction.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参数纠错视图
 */
@Data
public class ParamCorrectionVO {
    private Long id;
    private Long robotId;
    private String robotName;
    private Long userId;
    private String userNickname;
    private Long defId;
    private String paramName;
    private String oldValue;
    private String newValue;
    private String reason;
    /** 状态: 0待审核 1已采纳 2已拒绝 */
    private Integer status;
    private Long reviewerId;
    private String reviewNote;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}