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
        CompanyMember existing = findMember(companyId, userId);
        if (existing != null) {
            activateMember(existing, role);
            return;
        }

        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(1);
        try {
            companyMemberMapper.insert(member);
            log.info("添加企业成员: companyId={}, userId={}, role={}", companyId, userId, role);
        } catch (DuplicateKeyException e) {
            // Concurrent insert or historical disabled membership: preserve one row and activate it.
            CompanyMember concurrent = findMember(companyId, userId);
            if (concurrent == null) {
                throw e;
            }
            activateMember(concurrent, role);
        }
    }

    private CompanyMember findMember(Long companyId, Long userId) {
        return companyMemberMapper.selectOne(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getCompanyId, companyId)
                .eq(CompanyMember::getUserId, userId)
                .last("LIMIT 1"));
    }

    private void activateMember(CompanyMember member, String role) {
        member.setStatus(1);
        if ("OWNER".equals(role)) {
            member.setRole("OWNER");
        }
        companyMemberMapper.updateById(member);
        log.info("激活企业成员: companyId={}, userId={}, role={}",
                member.getCompanyId(), member.getUserId(), member.getRole());
    }
}
