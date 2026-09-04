package com.robot.home.article.vo;

import lombok.Data;

import java.util.List;

/**
 * 资讯详情
 */
@Data
public class ArticleDetailVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long robotId;
    private String robotName;
    private Long brandId;
    private String brandName;
    private String title;
    private String cover;
    private String summary;
    private String content;
    private String author;
    private String source;
    private List<String> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private java.time.LocalDateTime publishTime;
    /** 当前用户是否点赞 / 收藏 */
    private Boolean liked;
    private Boolean favorited;
    /** 上一篇 / 下一篇 */
    private ArticleNeighborVO prev;
    private ArticleNeighborVO next;
}
