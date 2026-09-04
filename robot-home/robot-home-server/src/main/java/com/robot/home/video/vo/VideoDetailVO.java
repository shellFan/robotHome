package com.robot.home.video.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频详情
 */
@Data
public class VideoDetailVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long robotId;
    private String robotName;
    private Long brandId;
    private String brandName;
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
    private Boolean liked;
    private Boolean favorited;
    /** 相关推荐 */
    private List<VideoListVO> related;
}
