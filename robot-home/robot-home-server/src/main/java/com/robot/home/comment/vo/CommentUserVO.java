package com.robot.home.comment.vo;

import lombok.Data;

/**
 * 评论中的用户信息（脱敏）
 */
@Data
public class CommentUserVO {

    private Long id;
    private String nickname;
    private String avatar;
}
