package com.robot.home.robot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品参数变更记录
 * 表名：product_change
 */
@Data
@TableName("product_change")
public class ProductChange extends IdEntity {

    /** 机器人ID */
    private Long robotId;
    /** 参数键 */
    private String paramKey;
    /** 旧值 */
    private String oldValue;
    /** 新值 */
    private String newValue;
    /** 来源URL */
    private String sourceUrl;
    /** 来源名称 */
    private String sourceName;
    /** 检测时间 */
    private LocalDateTime detectedTime;
    /** 创建时间 */
    private LocalDateTime createTime;
}