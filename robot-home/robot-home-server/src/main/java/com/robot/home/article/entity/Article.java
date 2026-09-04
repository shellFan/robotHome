package com.robot.home.article.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资讯文章
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article")
public class Article extends BaseEntity {

    private Long categoryId;
    private Long robotId;
    private Long brandId;
    private String title;
    private String cover;
    private String summary;
    private String content;
    private String author;
    private String source;
    private String tags;
    private Integer isTop;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    /** 0下架 1上架 */
    private Integer status;
    private LocalDateTime publishTime;
    /** 1示例数据 */
    private Integer isExample;
}
