package com.robot.home.company.service;

import com.robot.home.company.entity.CompanyMember;

import java.util.List;

/**
 * 企业成员服务
 * <p>
 * 核心安全职责：校验用户是否属于某个企业，防止企业身份冒充
 */
public interface CompanyMemberService {

    /**
     * 校验用户是否属于指定企业（status=1）
     *
     * @param userId    用户ID
     * @param companyId 企业ID
     * @return 成员记录，不存在返回null
     */
    CompanyMember getActiveMember(Long userId, Long companyId);

    /**
     * 校验用户是否属于指定企业，不属于则抛异常
     *
     * @param userId    用户ID
     * @param companyId 企业ID
     * @return 成员记录
     * @throws com.robot.home.common.exception.BusinessException 无权限时抛出
     */
    CompanyMember requireActiveMember(Long userId, Long companyId);

    /**
     * 获取用户所属的所有企业成员关系（status=1）
     *
     * @param userId 用户ID
     * @return 成员列表
     */
    List<CompanyMember> listActiveByUserId(Long userId);

    /**
     * 获取用户所属的第一个企业ID（用于单企业场景）
     *
     * @param userId 用户ID
     * @return 企业ID，无则返回null
     */
    Long getFirstCompanyId(Long userId);

    /**
     * 添加企业成员
     *
     * @param companyId 企业ID
     * @param userId    用户ID
     * @param role      角色
     */
    void addMember(Long companyId, Long userId, String role);
}