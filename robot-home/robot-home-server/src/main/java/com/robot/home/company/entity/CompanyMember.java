package com.robot.home.company.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;

/**
 * 企业成员关系
 * 表名：company_member
 * <p>
 * 用于校验用户是否属于某个企业，防止企业身份冒充
 */
@Data
@TableName("company_member")
public class CompanyMember extends BaseEntity {

    /** 企业ID */
    private Long companyId;

    /** 用户ID */
    private Long userId;

    /** 角色: OWNER/ADMIN/MEMBER */
    private String role;

    /** 0禁用 1正常 */
    private Integer status;
}