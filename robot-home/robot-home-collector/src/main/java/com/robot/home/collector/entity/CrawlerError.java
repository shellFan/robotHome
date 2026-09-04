package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采集错误
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_error")
public class CrawlerError extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private Long sourceId;

    /** 任务ID */
    private Long taskId;

    /** 相关URL */
    private String url;

    /** 错误类型: NETWORK/TIMEOUT/HTTP_403/HTTP_404/PARSE_ERROR/JS_RENDER_ERROR/DUPLICATE/DATABASE/IMAGE/BLOCKED/UNKNOWN */
    private String errorType;

    /** 错误信息 */
    private String errorMessage;

    /** 详细错误信息 */
    private String errorDetail;

    /** HTTP状态码 */
    private Integer httpStatus;

    /** 重试次数 */
    private Integer retryCount;

    /** 是否已解决: 0未解决 1已解决 */
    private Integer resolved;

    /** 解决备注 */
    private String resolveNote;
}