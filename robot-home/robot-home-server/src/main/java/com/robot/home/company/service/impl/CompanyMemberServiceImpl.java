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

    private static final java.util.Set<String> VALID_ROLES =
            new java.util.HashSet<>(java.util.Arrays.asList("OWNER", "ADMIN", "EDITOR", "SALES", "VIEWER"));

    @Override
    public List<CompanyMember> listMembers(Long companyId) {
        return companyMemberMapper.selectList(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getCompanyId, companyId)
                .eq(CompanyMember::getStatus, 1)
                .orderByAsc(CompanyMember::getCreateTime));
    }

    @Override
    public void updateMemberRole(Long operatorId, Long companyId, Long targetUserId, String newRole) {
        if (!VALID_ROLES.contains(newRole)) {
            throw new BusinessException("无效的角色: " + newRole);
        }
        requireOwner(operatorId, companyId);
        // 不能修改自己的角色（防止最后一个OWNER自我降级导致企业无OWNER）
        if (operatorId.equals(targetUserId)) {
            throw new BusinessException("不能修改自己的角色");
        }
        CompanyMember target = getActiveMember(targetUserId, companyId);
        if (target == null) {
            throw new BusinessException("目标用户不是该企业成员");
        }
        // 如果目标 是OWNER且要降级，须确保还有其他OWNER
        if ("OWNER".equals(target.getRole()) && !"OWNER".equals(newRole)) {
            ensureOtherOwnerExists(companyId, targetUserId);
        }
        int updated = companyMemberMapper.update(null, Wrappers.<CompanyMember>lambdaUpdate()
                .eq(CompanyMember::getId, target.getId())
                .eq(CompanyMember::getRole, target.getRole())
                .set(CompanyMember::getRole, newRole));
        if (updated != 1) {
            throw new BusinessException("成员角色已被其他操作修改，请刷新后重试");
        }
        log.info("成员角色更新: companyId={}, targetUserId={}, {} → {}, operator={}",
                companyId, targetUserId, target.getRole(), newRole, operatorId);
    }

    @Override
    public void removeMember(Long operatorId, Long companyId, Long targetUserId) {
        requireOwner(operatorId, companyId);
        // 不能移除自己
        if (operatorId.equals(targetUserId)) {
            throw new BusinessException("不能移除自己，如需退出请联系其他OWNER操作");
        }
        CompanyMember target = getActiveMember(targetUserId, companyId);
        if (target == null) {
            throw new BusinessException("目标用户不是该企业成员");
        }
        // 如果目标 是OWNER，须确保还有其他OWNER
        if ("OWNER".equals(target.getRole())) {
            ensureOtherOwnerExists(companyId, targetUserId);
        }
        // 逻辑移除：status=0，权限校验getActiveMember只查status=1，移除后立即失去访问权
        int updated = companyMemberMapper.update(null, Wrappers.<CompanyMember>lambdaUpdate()
                .eq(CompanyMember::getId, target.getId())
                .eq(CompanyMember::getStatus, 1)
                .set(CompanyMember::getStatus, 0));
        if (updated != 1) {
            throw new BusinessException("成员状态已被其他操作修改，请刷新后重试");
        }
        log.info("成员移除: companyId={}, targetUserId={}, operator={}",
                companyId, targetUserId, operatorId);
    }

    /**
     * 校验操作人是该企业的OWNER
     */
    private void requireOwner(Long operatorId, Long companyId) {
        CompanyMember operator = getActiveMember(operatorId, companyId);
        if (operator == null) {
            throw new BusinessException("您不是该企业的成员，无权操作");
        }
        if (!"OWNER".equals(operator.getRole())) {
            throw new BusinessException("仅企业OWNER可执行此操作");
        }
    }

    /**
     * 确保移除/降级目标OWNER后企业仍至少有一个OWNER
     */
    private void ensureOtherOwnerExists(Long companyId, Long excludingUserId) {
        Long otherOwners = companyMemberMapper.selectCount(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getCompanyId, companyId)
                .eq(CompanyMember::getRole, "OWNER")
                .eq(CompanyMember::getStatus, 1)
                .ne(CompanyMember::getUserId, excludingUserId));
        if (otherOwners == 0) {
            throw new BusinessException("不能移除或降级最后一个OWNER");
        }
    }
}
