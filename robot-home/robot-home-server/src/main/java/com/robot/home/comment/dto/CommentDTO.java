package com.robot.home.comment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 发表评论参数
 */
@Data
public class CommentDTO {

    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @NotNull(message = "业务 id 不能为空")
    private Long bizId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过 1000 字")
    private String content;

    /** 0 或 null 表示一级评论 */
    private Long parentId;

    /** 被回复的用户 id */
    private Long replyTo;
}
