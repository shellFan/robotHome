package com.robot.home.inquiry.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 询价 / 采购线索
 */
@Getter
@Setter
@TableName("inquiry")
public class Inquiry extends BaseEntity {

    private Long robotId;
    private String robotName;
    private Long userId;
    private String name;
    private String phone;
    /** 联系人姓名 */
    private String contactName;
    /** 联系邮箱 */
    private String email;
    /** 来源(pc/miniapp) */
    private String source;
    /** 分配给（后台管理员ID） */
    private Long assignedTo;
    private String region;
    /** 1个人 2企业 */
    private Integer customerType;
    private String companyName;
    private Integer quantity;
    private String budget;
    private String remark;
    /** 1待处理 2处理中 3已联系 4已成交 5已关闭 */
    private Integer status;
    private String handleNote;
    /** 跟进记录 JSON */
    private String handleRecords;
    /** 询价类型: GENERAL/PURCHASE/LEASE/COOPERATE */
    private String inquiryType;
    /** 需求类型: SPECIFIC具体型号/OPEN开放式 */
    private String requirementType;
    /** 机器人分类(开放式需求) */
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
    /** 采购场景: INDUSTRIAL/LOGISTICS/MEDICAL/EDUCATION/SERVICE/OTHER */
    private String procurementScene;
    /** 期望采购时间 */
    private String purchaseTime;
    /** 线索优先级: 0普通 1高 2紧急 */
    private Integer leadPriority;
    /** 优先级理由 */
    private String leadReason;

    @Override
    public String toString() {
        return "Inquiry{id=" + getId() + ", name=" + name + ", phone=" + phone + ", status=" + status + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inquiry inquiry = (Inquiry) o;
        return Objects.equals(getId(), inquiry.getId());
    }

    @Override
    public int hashCode() { return Objects.hash(getId()); }
}