package com.robot.home.collection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户收藏集
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_collection")
public class UserCollection extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String name;
    private String description;
    /** 可见性: PUBLIC/PRIVATE */
    private String visibility;
    private Integer robotCount;
}