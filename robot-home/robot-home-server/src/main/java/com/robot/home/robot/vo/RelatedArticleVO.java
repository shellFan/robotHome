package com.robot.home.robot.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 详情中关联的资讯 / 视频简项
 */
@Data
public class RelatedArticleVO {

    private Long id;
    private String title;
    private String cover;
    private String summary;
    private LocalDateTime publishTime;
    private Integer viewCount;
}
