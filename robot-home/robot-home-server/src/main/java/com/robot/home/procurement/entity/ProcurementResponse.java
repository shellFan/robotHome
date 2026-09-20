package com.robot.home.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购需求响应（企业报价/方案）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("procurement_response")
public class ProcurementResponse extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long procurementId;
    private Long companyId;
    private Long contactUserId;
    private String solution;
    /** 推荐机器人ID JSON数组 */
    private String robotIds;
    private String priceDescription;
    private String deliveryDescription;
    private String contactDescription;
    /** 状态: PENDING/ACCEPTED/REJECTED */
    private String status;
    /** 事件唯一键（用于去重） */
    private String eventKey;
}