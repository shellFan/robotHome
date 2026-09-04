package com.robot.home.community.vo;

import lombok.Data;

/**
 * 圈子视图（含实时帖子数）
 */
@Data
public class CircleVO {

    private Long id;
    private String name;
    private String logo;
    private String description;
    private Integer postCount;
    private Integer followCount;
    private Integer sort;
    /** 当前用户是否关注该圈子（圈子以 brand 形式被关注，此处预留） */
    private Boolean followed;
}
