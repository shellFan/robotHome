package com.robot.home.trust.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据来源 VO（公开，不暴露内部信息）
 */
@Data
public class RobotDataSourceVO {

    private Long id;
    /** 来源类型: OFFICIAL/VENDOR_SITE/TRUSTED_MEDIA/MANUAL/USER_CORRECTION/CRAWLER */
    private String sourceType;
    private String sourceName;
    private String sourceUrl;
    /** 信任权重 0-100 */
    private Integer trustWeight;
    /** 是否已验证 */
    private Integer verified;
    private LocalDateTime verifiedTime;
}