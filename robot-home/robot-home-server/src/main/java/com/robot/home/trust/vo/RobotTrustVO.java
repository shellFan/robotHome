package com.robot.home.trust.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 机器人信任度 VO
 */
@Data
public class RobotTrustVO {

    /** 信任等级: VERIFIED/HIGH/NORMAL/LOW */
    private String level;
    /** 信任分 0-100 */
    private int score;
    /** 可信原因列表 */
    private List<String> reasonList;
    /** 最后验证时间 */
    private LocalDateTime lastVerifiedTime;
    /** 数据来源数量 */
    private int sourceCount;
    /** 待处理纠错数 */
    private int pendingCorrectionCount;
}