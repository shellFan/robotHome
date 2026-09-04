package com.robot.home.tutorial.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教程详情（Markdown 正文）
 */
@Data
public class TutorialDetailVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String cover;
    private String summary;
    private String content;
    private String author;
    private List<String> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime publishTime;
    private Boolean liked;
    private Boolean favorited;
    private List<TutorialListVO> related;
}
