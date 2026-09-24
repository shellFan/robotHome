package com.robot.home.discovery.vo;

import lombok.Data;

/**
 * 话题简要信息（发现页用）
 */
@Data
public class TopicVO {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer postCount;
}