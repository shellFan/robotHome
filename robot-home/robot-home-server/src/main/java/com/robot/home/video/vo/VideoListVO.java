package com.robot.home.video.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频列表项
 */
@Data
public class VideoListVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String cover;
    private String url;
    private Integer duration;
    private String summary;
    private String author;
    private List<String> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime publishTime;
}
