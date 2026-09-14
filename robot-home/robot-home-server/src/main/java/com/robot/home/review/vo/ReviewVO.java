package com.robot.home.review.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 口碑评价视图
 */
@Data
public class ReviewVO {

    private Long id;
    private Long robotId;
    /** 评价用户信息 */
    private ReviewUserVO user;
    private Integer overallScore;
    private Integer qualityScore;
    private Integer serviceScore;
    private Integer costScore;
    private String content;
    /** 图片URL列表 */
    private List<String> imageList;
    private Integer helpfulCount;
    /** 当前用户是否已投有用 */
    private Boolean helpfuled;
    /** 当前用户是否可编辑 */
    private Boolean canEdit;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    /** 评价状态: 0待审核 1已通过 2已拒绝 */
    private Integer status;
}