package com.robot.home.advertising.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 广告事件流水(只增不改,无逻辑删除)
 */
@Data
@TableName("advertising_event")
public class AdvertisingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long campaignId;
    private Long slotId;
    /** 事件类型: IMPRESSION/CLICK */
    private String eventType;
    private String visitorId;
    private String ip;
    /** 去重键 */
    private String dedupKey;
    private LocalDateTime eventTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}