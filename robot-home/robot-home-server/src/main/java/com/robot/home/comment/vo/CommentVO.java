package com.robot.home.comment.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图：一级评论 + 回复列表
 */
@Data
public class CommentVO {

    private Long id;
    private String bizType;
    private Long bizId;
    private CommentUserVO user;
    private String content;
    private Long parentId;
    /** 被回复人昵称 */
    private String replyToNickname;
    private Integer likeCount;
    private Integer replyCount;
    /** 当前用户是否已点赞 */
    private Boolean liked;
    /** 当前用户是否可删除（本人或管理员） */
    private Boolean canDelete;
    private LocalDateTime createTime;
    /** 回复列表（一级评论返回前 N 条） */
    private List<CommentVO> replies;
}
