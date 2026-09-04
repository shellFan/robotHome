package com.robot.home.comment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用评论：robot / article / video / post / tutorial 共用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {

    /** 业务类型：robot/article/video/post/tutorial */
    private String bizType;
    private Long bizId;
    private Long userId;
    private String content;
    /** 0 一级评论，否则为回复的父评论 id */
    private Long parentId;
    /** 被回复用户 id */
    private Long replyTo;
    private Integer likeCount;
    private Integer replyCount;
    private Integer status;
}
