package com.robot.home.user.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户主页信息（对外脱敏）
 * Phase9增强：贡献统计+贡献等级
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileVO extends UserVO {

    private String intro;
    private Integer fansCount;
    private Integer followCount;
    private Integer postCount;
    /** Phase9: 贡献分 */
    private Integer contributionScore;
    /** Phase9: 评测数 */
    private Integer reviewCount;
    /** Phase9: 提问数 */
    private Integer questionCount;
    /** Phase9: 回答数 */
    private Integer answerCount;
    /** Phase9: 贡献等级 */
    private String contributorLevel;
    /** Phase9: 当前用户是否已关注此用户 */
    private Boolean followed;
}
