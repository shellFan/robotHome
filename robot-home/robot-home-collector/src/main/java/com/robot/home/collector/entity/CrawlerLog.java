package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采集日志
 */
@Data
@TableName("crawler_log")
public class CrawlerLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id")
    private Long id;

    /** 数据源ID */
    private Long sourceId;

    /** 任务ID */
    private Long taskId;

    /** 相关URL */
    private String url;

    /** 动作 */
    private String action;

    /** 日志级别: INFO/WARN/ERROR */
    private String level;

    /** 日志信息 */
    private String message;

    /** 耗时(毫秒) */
    private Integer duration;

    /** HTTP状态码 */
    private Integer httpStatus;

    /** 结果: SUCCESS/FAILED/SKIPPED */
    private String result;

    /** 额外数据JSON */
    private String extraData;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}