package com.robot.home.topic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 话题
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("topic")
public class Topic extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    /** 状态: 0禁用 1正常 */
    private Integer status;
    private Integer postCount;
}