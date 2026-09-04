package com.robot.home.recommend.vo;

import lombok.Data;

/**
 * 推荐位对外视图
 */
@Data
public class RecommendItemVO {

    private Long id;
    private String bizType;
    private Long bizId;
    private String title;
    private String image;
    private String url;
    private Integer sort;
}
