package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 采集任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_task")
public class CrawlerTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private Long sourceId;

    /** 任务类型: FULL/INCREMENTAL/SINGLE_URL/RETRY */
    private String taskType;

    /** 任务状态: PENDING/RUNNING/COMPLETED/FAILED/STOPPED */
    private String status;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 发现URL数 */
    private Integer urlsDiscovered;

    /** 成功URL数 */
    private Integer urlsSuccess;

    /** 失败URL数 */
    private Integer urlsFailed;

    /** 新增文章数 */
    private Integer articlesNew;

    /** 更新文章数 */
    private Integer articlesUpdated;

    /** 重复文章数 */
    private Integer articlesDuplicate;

    /** 新增产品数 */
    private Integer productsNew;

    /** 更新产品数 */
    private Integer productsUpdated;

    /** 下载图片数 */
    private Integer imagesDownloaded;

    /** 失败图片数 */
    private Integer imagesFailed;

    /** 错误信息 */
    private String errorMessage;
}