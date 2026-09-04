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
}
