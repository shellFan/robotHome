package com.robot.home.behavior.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 行为事件提交DTO
 */
@Data
public class BehaviorEventDTO {

    /** 事件类型 */
    @NotBlank(message = "事件类型不能为空")
    @Size(max = 32)
    private String eventType;

    /** 业务类型 */
    @Size(max = 16)
    private String bizType;

    /** 业务对象ID */
    private Long bizId;

    /** 扩展信息 */
    @Size(max = 512)
    private String extra;
}