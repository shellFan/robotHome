package com.robot.home.company.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.company.entity.CompanyMember;
import com.robot.home.company.mapper.CompanyMemberMapper;
import com.robot.home.company.service.CompanyMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 企业成员服务实现
 * <p>
 * 核心安全职责：校验用户是否属于某个企业，防止企业身份冒充
 */
@Slf4j
@Service
public class CompanyMemberServiceImpl implements CompanyMemberService {

    @Resource
    private CompanyMemberMapper companyMemberMapper;

    @Override
    public CompanyMember getActiveMember(Long userId, Long companyId) {
        return companyMemberMapper.selectOne(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getUserId, userId)
                .eq(CompanyMember::getCompanyId, companyId)
                .eq(CompanyMember::getStatus, 1));
    }

    @Override
    public CompanyMember requireActiveMember(Long userId, Long companyId) {
        CompanyMember member = getActiveMember(userId, companyId);
        if (member == null) {
            throw new BusinessException("您不是该企业的成员，无权操作");
        }
        return member;
    }

    @Override
    public List<CompanyMember> listActiveByUserId(Long userId) {
        return companyMemberMapper.selectList(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getUserId, userId)
                .eq(CompanyMember::getStatus, 1)
                .orderByDesc(CompanyMember::getCreateTime));
    }

    @Override
    public Long getFirstCompanyId(Long userId) {
        CompanyMember member = companyMemberMapper.selectOne(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getUserId, userId)
                .eq(CompanyMember::getStatus, 1)
                .orderByDesc(CompanyMember::getCreateTime)
                .last("LIMIT 1"));
        return member != null ? member.getCompanyId() : null;
    }

    @Override
    public void addMember(Long companyId, Long userId, String role) {
        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(1);
        try {
            companyMemberMapper.insert(member);
            log.info("添加企业成员: companyId={}, userId={}, role={}", companyId, userId, role);
        } catch (DuplicateKeyException e) {
            // uk_company_user 唯一约束，已存在则忽略
            log.info("企业成员已存在: companyId={}, userId={}", companyId, userId);
        }
    }
}