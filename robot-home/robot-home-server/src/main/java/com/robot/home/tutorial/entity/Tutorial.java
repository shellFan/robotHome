package com.robot.home.tutorial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 教程（Markdown 正文）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tutorial")
public class Tutorial extends BaseEntity {

    private Long categoryId;
    private String title;
    private String cover;
    private String summary;
    private String content;
    private String author;
    private String tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Integer status;
    private LocalDateTime publishTime;
    private Integer isExample;
}
