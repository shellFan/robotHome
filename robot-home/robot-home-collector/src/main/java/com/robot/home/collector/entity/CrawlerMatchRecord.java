package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 匹配记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_match_record")
public class CrawlerMatchRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private Long sourceId;

    /** 业务类型: ARTICLE/PRODUCT */
    private String bizType;

    /** 业务ID(crawler_article/crawler_product) */
    private Long bizId;

    /** 匹配类型: BRAND/COMPANY/ROBOT */
    private String matchType;

    /** 匹配目标ID */
    private Long matchTargetId;

    /** 匹配关键词 */
    private String matchKeyword;

    /** 匹配置信度 */
    private BigDecimal confidence;

    /** 匹配状态: PENDING/AUTO_MATCHED/MANUAL_MATCHED/REJECTED */
    private String matchStatus;

    /** 处理人 */
    private Long handledBy;

    /** 处理时间 */
    private LocalDateTime handleTime;
}