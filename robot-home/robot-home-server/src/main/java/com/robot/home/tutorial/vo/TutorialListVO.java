package com.robot.home.tutorial.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教程列表项
 */
@Data
public class TutorialListVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String cover;
    private String summary;
    private String author;
    private List<String> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime publishTime;
}
