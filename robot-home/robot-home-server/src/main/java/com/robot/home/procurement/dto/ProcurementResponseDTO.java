package com.robot.home.procurement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 采购需求响应提交DTO
 */
@Data
public class ProcurementResponseDTO {

    /** 采购需求ID */
    @NotNull(message = "采购需求ID不能为空")
    private Long procurementId;

    /** 解决方案 */
    @NotBlank(message = "解决方案不能为空")
    private String solution;

    /** 推荐机器人ID JSON数组，如 "[1,2,3]" */
    private String robotIds;

    /** 价格说明 */
    private String priceDescription;

    /** 交付说明 */
    private String deliveryDescription;

    /** 联系方式 */
    @NotBlank(message = "联系方式不能为空")
    private String contactDescription;
}