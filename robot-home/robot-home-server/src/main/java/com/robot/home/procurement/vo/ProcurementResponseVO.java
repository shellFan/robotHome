package com.robot.home.procurement.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购需求响应视图对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcurementResponseVO {

    private Long id;
    private Long procurementId;
    /** 企业ID */
    private Long companyId;
    /** 企业名称 */
    private String companyName;
    /** 企业logo */
    private String companyLogo;
    /** 联系人用户ID */
    private Long contactUserId;
    /** 解决方案（脱敏：非本人/管理员不展示） */
    private String solution;
    /** 推荐机器人ID JSON数组 */
    private String robotIds;
    /** 价格说明（脱敏） */
    private String priceDescription;
    /** 交付说明（脱敏） */
    private String deliveryDescription;
    /** 联系方式（脱敏：非本人/管理员不展示） */
    private String contactDescription;
    /** 状态 */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 是否为当前用户的响应（用户端列表用） */
    private Boolean isMine;
    /** 是否可查看完整信息（隐私控制） */
    private Boolean canViewDetail;
}