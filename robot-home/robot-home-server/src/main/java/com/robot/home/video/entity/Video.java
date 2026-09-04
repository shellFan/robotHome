package com.robot.home.video.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 视频
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video")
public class Video extends BaseEntity {

    private Long categoryId;
    private Long robotId;
    private Long brandId;
    private String title;
    private String cover;
    private String url;
    private Integer duration;
    private String summary;
    private String author;
    private String tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    /** 0下架 1上架 */
    private Integer status;
    private LocalDateTime publishTime;
    private Integer isExample;
}
