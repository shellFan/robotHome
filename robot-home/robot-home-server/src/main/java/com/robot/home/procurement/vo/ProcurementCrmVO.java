package com.robot.home.procurement.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购CRM视图对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcurementCrmVO {

    private Long id;
    /** 询价/采购线索ID */
    private Long inquiryId;
    /** 联系人姓名 */
    private String name;
    /** 联系电话（脱敏） */
    private String phone;
    /** 联系邮箱（脱敏） */
    private String email;
    /** 公司名称 */
    private String companyName;
    /** 机器人名称 */
    private String robotName;
    /** 采购场景 */
    private String procurementScene;
    /** 使用场景 */
    private String usageScene;
    /** 需求类型: SPECIFIC/OPEN */
    private String requirementType;
    /** 预算 */
    private String budget;
    /** 数量 */
    private Integer quantity;
    /** 备注 */
    private String remark;
    /** 线索评分 */
    private Integer leadScore;
    /** 线索优先级 */
    private Integer leadPriority;

    /** CRM Pipeline状态 */
    private String pipelineStatus;
    /** CRM负责人ID */
    private Long crmOwner;
    /** CRM负责人姓名 */
    private String crmOwnerName;
    /** CRM优先级 */
    private Integer crmPriority;
    /** 下次跟进时间 */
    private LocalDateTime nextFollowTime;
    /** 最后跟进时间 */
    private LocalDateTime lastFollowTime;
    /** CRM来源 */
    private String crmSource;
    /** CRM备注 */
    private String crmRemark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 跟进记录列表（详情时填充） */
    private List<FollowRecordVO> followRecords;
    /** 响应列表（详情时填充） */
    private List<ProcurementResponseVO> responses;

    /**
     * 跟进记录简要视图
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FollowRecordVO {
        private Long id;
        private Long procurementId;
        private String operatorName;
        private String action;
        private String content;
        private String oldStatus;
        private String newStatus;
        private LocalDateTime nextFollowTime;
        private LocalDateTime createTime;
    }
}