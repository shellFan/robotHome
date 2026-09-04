package com.robot.home.recommend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 推荐位内容项
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommend_item")
public class RecommendItem extends BaseEntity {

    private Long positionId;
    private String bizType;
    private Long bizId;
    private String title;
    private String image;
    private String url;
    private Integer sort;
    private Integer status;
}
