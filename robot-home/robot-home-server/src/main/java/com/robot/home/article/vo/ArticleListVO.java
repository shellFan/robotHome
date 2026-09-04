package com.robot.home.article.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯列表项
 */
@Data
public class ArticleListVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String cover;
    private String summary;
    private String author;
    private String source;
    private List<String> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Integer isTop;
    private LocalDateTime publishTime;
}
