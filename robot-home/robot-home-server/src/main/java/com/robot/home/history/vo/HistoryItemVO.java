package com.robot.home.history.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 浏览历史条目（回填业务标题与图片）
 */
@Data
public class HistoryItemVO {

    private Long id;
    private String bizType;
    private Long bizId;
    private String title;
    private String subtitle;
    private String image;
    private String url;
    private LocalDateTime createTime;
}
