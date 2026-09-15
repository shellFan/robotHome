package com.robot.home.inquiry.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 询价视图
 */
@Data
public class InquiryVO {

    private Long id;
    private Long robotId;
    private String robotName;
    private Long userId;
    private String name;
    /** 列表场景下手机号脱敏 */
    private String phone;
    private String region;
    private Integer customerType;
    private String customerTypeName;
    private String companyName;
    private Integer quantity;
    private String budget;
    private String remark;
    private Integer status;
    private String statusName;
    private String handleNote;
    private LocalDateTime createTime;
    /** 询价类型: GENERAL/PURCHASE/LEASE/COOPERATE */
    private String inquiryType;
    /** 采购场景 */
    private String procurementScene;
    /** 期望采购时间 */
    private String purchaseTime;
    /** 线索优先级: 0普通 1高 2紧急 */
    private Integer leadPriority;
    /** 优先级理由 */
    private String leadReason;
    /** 需求类型: SPECIFIC/OPEN */
    private String requirementType;
    /** 机器人分类 */
    private String category;
    /** 使用场景 */
    private String usageScene;
    /** 技术要求 */
    private String technicalRequirements;
    /** 需要演示 */
    private Integer needDemo;
    /** 需要方案 */
    private Integer needSolution;
    /** 线索评分 */
    private Integer leadScore;
    /** 分配给 */
    private Long assignedTo;
}
