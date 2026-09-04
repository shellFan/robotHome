package com.robot.home.community.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子视图
 */
@Data
public class PostVO {

    private Long id;
    private Long circleId;
    private String circleName;
    private PostAuthorVO author;
    private String title;
    private String content;
    private List<String> images;
    private String videoUrl;
    private Long robotId;
    private String robotName;
    private String robotCover;
    private Long brandId;
    private String brandName;
    private String topic;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private Integer isTop;
    private Integer status;
    private LocalDateTime createTime;
    private Boolean liked;
    private Boolean favorited;
    private Boolean canDelete;
}
