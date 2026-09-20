package com.robot.home.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 社区帖子
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_post")
public class CommunityPost extends BaseEntity {

    private Long circleId;
    /** 帖子类型: discussion/question/diy/review/news_share */
    private String postType;
    private Long userId;
    private String title;
    private String content;
    /** 图片 JSON 数组 */
    private String images;
    private String videoUrl;
    private Long robotId;
    private Long brandId;
    private Long companyId;
    private String topic;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private Integer hotScore;
    private Integer isTop;
    /** Phase10: 话题ID */
    private Long topicId;
    /** 0待审 1正常 2下架 */
    private Integer status;
}
