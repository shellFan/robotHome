package com.robot.home.reputation.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * P0-7: 信誉视图对象
 */
@Data
public class ReputationVO {

    private Long userId;

    /** 用户昵称（冗余填充） */
    private String nickname;

    /** 用户头像 */
    private String avatar;

    /** 信誉分 */
    private Integer reputationScore;

    /** 信誉等级: NEW/CONTRIBUTOR/ACTIVE/TRUSTED/EXPERT */
    private String reputationLevel;

    /** 等级中文描述 */
    private String levelLabel;

    /** 下一等级所需分数（0表示已满级） */
    private Integer nextLevelScore;

    /** 距下一等级还差分数 */
    private Integer scoreToNextLevel;

    /** 被采纳纠错数 */
    private Integer acceptedCorrections;

    /** 被采纳回答数 */
    private Integer acceptedAnswers;

    /** 有帮助评测数 */
    private Integer helpfulReviews;

    /** 高质量帖子数 */
    private Integer qualityPosts;

    /** 惩罚分 */
    private Integer penaltyScore;

    /** 最近事件（最多5条） */
    private List<ReputationEventVO> recentEvents;

    /** 更新时间 */
    private LocalDateTime updateTime;
}