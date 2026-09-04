package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布到主站 article 表的实体（采集器侧映射）
 */
@Data
@TableName("article")
public class PublishArticle {

    private Long id;
    private Long categoryId;
    private Long robotId;
    private Long brandId;
    private String title;
    private String cover;
    private String summary;
    private String content;
    private String author;
    private String source;
    /** 来源URL */
    private String sourceUrl;
    private String tags;
    private Integer isTop;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    /** 0下架 1上架 */
    private Integer status;
    private LocalDateTime publishTime;
    private Integer isExample;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}