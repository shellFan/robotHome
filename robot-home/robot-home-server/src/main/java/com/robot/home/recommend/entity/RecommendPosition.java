package com.robot.home.recommend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 推荐位
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommend_position")
public class RecommendPosition extends BaseEntity {

    /** hot_robot / new_robot / recommend_article ... */
    private String code;
    private String name;
    private String bizType;
    private Integer status;
}
