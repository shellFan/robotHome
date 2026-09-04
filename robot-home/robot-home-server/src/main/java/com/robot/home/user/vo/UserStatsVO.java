package com.robot.home.user.vo;

import lombok.Data;

/**
 * 用户中心统计
 */
@Data
public class UserStatsVO {

    private Long favoriteCount;
    private Long historyCount;
    private Long postCount;
    private Long commentCount;
    private Long likeCount;
    private Long followCount;
    private Long inquiryCount;
    private Long unreadMessageCount;
    private Long fansCount;
}
