package com.robot.home.advertising.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 广告创意素材
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("advertising_creative")
public class AdvertisingCreative extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long campaignId;
    /** 素材类型: IMAGE/VIDEO */
    private String mediaType;
    private String mediaUrl;
    private String jumpUrl;
    private String title;
    private String description;
}