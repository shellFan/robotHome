package com.robot.home.favorite.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的收藏条目（按业务类型回填标题与图片）
 */
@Data
public class FavoriteItemVO {

    private Long id;
    private String bizType;
    private Long bizId;
    private String title;
    private String subtitle;
    private String image;
    /** 前端跳转路径，如 /robot/1 */
    private String url;
    private LocalDateTime createTime;
}
