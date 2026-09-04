package com.robot.home.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 社区圈子
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_circle")
public class CommunityCircle extends BaseEntity {

    private String name;
    private String logo;
    private String description;
    private Integer postCount;
    private Integer followCount;
    private Integer sort;
    private Integer status;
}
