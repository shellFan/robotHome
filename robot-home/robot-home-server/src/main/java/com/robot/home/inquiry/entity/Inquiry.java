package com.robot.home.inquiry.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 询价 / 采购线索
 */
@Data
@EqualsAndHashCode(callSuper = true)
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
}
