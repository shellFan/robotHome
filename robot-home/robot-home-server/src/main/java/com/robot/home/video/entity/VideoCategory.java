package com.robot.home.video.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频频道
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_category")
public class VideoCategory extends BaseEntity {

    private String name;
    private Integer sort;
    private Integer status;
}
