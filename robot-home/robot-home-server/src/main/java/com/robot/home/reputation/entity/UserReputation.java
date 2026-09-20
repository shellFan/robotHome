package com.robot.home.reputation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信誉（不继承基类，自定义主键）
 */
@Data
@TableName("user_reputation")
public class UserReputation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    /** 信誉分 */
    private Integer reputationScore;
    /** 信誉等级: BRONZE/SILVER/GOLD/PLATINUM/DIAMOND */
    private String reputationLevel;
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
    /** 每日贡献上限 */
    private Integer dailyContribCap;
    private LocalDateTime updateTime;
}