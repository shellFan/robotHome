package com.robot.home.inquiry.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;

import java.util.Objects;

/**
 * 询价 / 采购线索
 */
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
    /** 采购场景: INDUSTRIAL/LOGISTICS/MEDICAL/EDUCATION/SERVICE/OTHER */
    private String procurementScene;
    /** 期望采购时间 */
    private String purchaseTime;
    /** 线索优先级: 0普通 1高 2紧急 */
    private Integer leadPriority;
    /** 优先级理由 */
    private String leadReason;

    // --- getter/setter ---
    public Long getRobotId() { return robotId; }
    public void setRobotId(Long robotId) { this.robotId = robotId; }

    public String getRobotName() { return robotName; }
    public void setRobotName(String robotName) { this.robotName = robotName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Integer getCustomerType() { return customerType; }
    public void setCustomerType(Integer customerType) { this.customerType = customerType; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getHandleNote() { return handleNote; }
    public void setHandleNote(String handleNote) { this.handleNote = handleNote; }

    public String getHandleRecords() { return handleRecords; }
    public void setHandleRecords(String handleRecords) { this.handleRecords = handleRecords; }

    public String getInquiryType() { return inquiryType; }
    public void setInquiryType(String inquiryType) { this.inquiryType = inquiryType; }

    public String getProcurementScene() { return procurementScene; }
    public void setProcurementScene(String procurementScene) { this.procurementScene = procurementScene; }

    public String getPurchaseTime() { return purchaseTime; }
    public void setPurchaseTime(String purchaseTime) { this.purchaseTime = purchaseTime; }

    public Integer getLeadPriority() { return leadPriority; }
    public void setLeadPriority(Integer leadPriority) { this.leadPriority = leadPriority; }

    public String getLeadReason() { return leadReason; }
    public void setLeadReason(String leadReason) { this.leadReason = leadReason; }

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