package com.robot.home.feed.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Phase9: 关注动态Feed项
 */
@Data
public class FeedItemVO {

    /** 动态ID（用于cursor分页） */
    private Long id;
    /** 动态类型: POST/REVIEW/QUESTION/ANSWER/ARTICLE/NEW_ROBOT */
    private String feedType;
    /** 目标ID（帖子/评测/问题/文章/机器人ID） */
    private Long targetId;
    /** 目标标题 */
    private String title;
    /** 目标摘要 */
    private String summary;
    /** 目标封面图 */
    private String coverImage;
    /** 来源类型: ROBOT/BRAND/COMPANY/USER */
    private String sourceType;
    /** 来源ID */
    private Long sourceId;
    /** 来源名称 */
    private String sourceName;
    /** 作者昵称 */
    private String authorNickname;
    /** 作者头像 */
    private String authorAvatar;
    /** 互动数（点赞/评论/浏览等，按类型不同） */
    private Integer interactionCount;
    /** 发布时间 */
    private LocalDateTime createTime;
}