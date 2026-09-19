package com.robot.home.user.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 用户公开主页信息（独立定义，不继承UserVO，防止敏感字段泄露）
 * Phase9增强：贡献统计+贡献等级
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileVO {

    private Long id;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String province;
    private String city;
    /** 个人简介 */
    private String intro;
    /** 粉丝数 */
    private Integer fansCount;
    /** 关注数 */
    private Integer followCount;
    /** 帖子数 */
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