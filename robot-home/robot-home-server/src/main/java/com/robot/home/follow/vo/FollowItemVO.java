package com.robot.home.follow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的关注条目（回填被关注对象信息）
 */
@Data
public class FollowItemVO {

    private Long id;
    /** user / brand / company / robot */
    private String followType;
    private Long followId;
    private String name;
    private String avatar;
    private String description;
    private String url;
    private LocalDateTime createTime;
}
