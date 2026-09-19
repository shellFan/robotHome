package com.robot.home.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Phase9: 用户贡献统计
 * 对应表: user_contribution_stat
 */
@Data
@TableName("user_contribution_stat")
public class UserContributionStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 事件类型: REVIEW_CREATE/POST_CREATE/QUESTION_CREATE/ANSWER_CREATE/ANSWER_ACCEPTED/CORRECTION_ACCEPTED/HELPFUL_RECEIVED */
    private String eventType;

    /** 事件次数 */
    private Integer eventCount;

    /** 贡献分合计 */
    private Integer contributionScore;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}