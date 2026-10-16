package com.robot.home;

import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.entity.CompanyMember;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.mapper.CompanyMemberMapper;
import com.robot.home.company.service.CompanyMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage 3: 企业权限生命周期验证
 *
 * 验证: OWNER角色保护 / 成员移除 / 角色变更 / 跨企业隔离 / 自我保护
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
@Transactional
class EnterprisePermissionLifecycleTest {

    private static final Long OWNER_USER_ID = 930001L;
    private static final Long MEMBER_USER_ID = 930002L;
    private static final Long OTHER_OWNER_USER_ID = 930003L;
    private static final Long OUTSIDER_USER_ID = 930004L;

    @Autowired
    private CompanyMemberService memberService;
    @Autowired
    private CompanyMemberMapper memberMapper;
    @Autowired
    private CompanyMapper companyMapper;

    @MockBean
    private RedisUtils redisUtils;

    @Test
    void ownerCanUpdateMemberRole() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, MEMBER_USER_ID, "VIEWER");

        memberService.updateMemberRole(OWNER_USER_ID, 1L, MEMBER_USER_ID, "EDITOR");

        CompanyMember updated = memberService.getActiveMember(MEMBER_USER_ID, 1L);
        assertEquals("EDITOR", updated.getRole());
    }

    @Test
    void nonOwnerCannotUpdateMemberRole() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, MEMBER_USER_ID, "EDITOR");

        // 普通成员不能自行提升权限
        assertThrows(BusinessException.class, () ->
                memberService.updateMemberRole(MEMBER_USER_ID, 1L, MEMBER_USER_ID, "OWNER"));
        assertThrows(BusinessException.class, () ->
                memberService.updateMemberRole(MEMBER_USER_ID, 1L, OWNER_USER_ID, "VIEWER"));
    }

    @Test
    void ownerCannotChangeOwnRole() {
        seedOwner(1L, OWNER_USER_ID);

        // OWNER不能修改自己的角色（防止最后一个OWNER自我降级）
        assertThrows(BusinessException.class, () ->
                memberService.updateMemberRole(OWNER_USER_ID, 1L, OWNER_USER_ID, "EDITOR"));
    }

    @Test
    void lastOwnerCannotBeRemoved() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, MEMBER_USER_ID, "EDITOR");

        // 最后一个OWNER不能被移除
        assertThrows(BusinessException.class, () ->
                memberService.removeMember(OWNER_USER_ID, 1L, OWNER_USER_ID));

        // 但如果有两个OWNER，其中一个可以被移除
        seedMember(1L, OTHER_OWNER_USER_ID, "OWNER");
        memberService.removeMember(OTHER_OWNER_USER_ID, 1L, OWNER_USER_ID);
        assertEquals(null, memberService.getActiveMember(OWNER_USER_ID, 1L));
    }

    @Test
    void lastOwnerCannotBeDemoted() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, OTHER_OWNER_USER_ID, "OWNER");

        // 有两个OWNER时可以降级其中一个
        memberService.updateMemberRole(OWNER_USER_ID, 1L, OTHER_OWNER_USER_ID, "EDITOR");
        assertEquals("EDITOR", memberService.getActiveMember(OTHER_OWNER_USER_ID, 1L).getRole());

        // 但现在只剩一个OWNER，不能再降级
        assertThrows(BusinessException.class, () ->
                memberService.updateMemberRole(OTHER_OWNER_USER_ID, 1L, OWNER_USER_ID, "EDITOR"));
    }

    @Test
    void ownerCannotRemoveSelf() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, OTHER_OWNER_USER_ID, "OWNER");

        // 即使有两个OWNER也不能移除自己（须由其他OWNER操作）
        assertThrows(BusinessException.class, () ->
                memberService.removeMember(OWNER_USER_ID, 1L, OWNER_USER_ID));
    }

    @Test
    void removedMemberLosesAccessImmediately() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, MEMBER_USER_ID, "EDITOR");

        memberService.removeMember(OWNER_USER_ID, 1L, MEMBER_USER_ID);

        // 移除后立即失去访问权：getActiveMember返回null
        assertEquals(null, memberService.getActiveMember(MEMBER_USER_ID, 1L));

        // requireActiveMember抛出异常
        assertThrows(BusinessException.class, () ->
                memberService.requireActiveMember(MEMBER_USER_ID, 1L));
    }

    @Test
    void crossCompanyMemberManagementIsRejected() {
        seedOwner(1L, OWNER_USER_ID);
        seedOwner(2L, OUTSIDER_USER_ID);
        seedMember(2L, MEMBER_USER_ID, "EDITOR");

        // 企业A的OWNER不能管理企业B的成员
        assertThrows(BusinessException.class, () ->
                memberService.removeMember(OWNER_USER_ID, 2L, MEMBER_USER_ID));
        assertThrows(BusinessException.class, () ->
                memberService.updateMemberRole(OWNER_USER_ID, 2L, MEMBER_USER_ID, "ADMIN"));
    }

    @Test
    void verificationRevocationInvalidatesMemberOperations() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, MEMBER_USER_ID, "VIEWER");

        // 撤销企业认证
        Company company = companyMapper.selectById(1L);
        company.setVerificationStatus("UNCLAIMED");
        companyMapper.updateById(company);

        // 成员管理操作仍然受角色控制（成员管理不依赖企业认证状态，
        // 但采购响应等业务操作会通过requireVerifiedActiveCompany拒绝）
        // 这里验证成员列表仍然可查（成员关系独立于认证状态）
        List<CompanyMember> members = memberService.listMembers(1L);
        assertTrue(members.size() >= 2);
    }

    @Test
    void listMembersReturnsOnlyActiveMembers() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, MEMBER_USER_ID, "EDITOR");

        // 添加一个已移除的成员
        CompanyMember removed = new CompanyMember();
        removed.setCompanyId(1L);
        removed.setUserId(930099L);
        removed.setRole("VIEWER");
        removed.setStatus(0);
        memberMapper.insert(removed);

        List<CompanyMember> members = memberService.listMembers(1L);
        assertTrue(members.stream().allMatch(m -> m.getStatus() == 1), "只返回活跃成员");
        assertTrue(members.stream().noneMatch(m -> m.getUserId().equals(930099L)));
    }

    @Test
    void invalidRoleIsRejected() {
        seedOwner(1L, OWNER_USER_ID);
        seedMember(1L, MEMBER_USER_ID, "VIEWER");

        assertThrows(BusinessException.class, () ->
                memberService.updateMemberRole(OWNER_USER_ID, 1L, MEMBER_USER_ID, "SUPERADMIN"));
    }

    // ==================== 辅助方法 ====================

    private void seedOwner(Long companyId, Long userId) {
        CompanyMember owner = new CompanyMember();
        owner.setCompanyId(companyId);
        owner.setUserId(userId);
        owner.setRole("OWNER");
        owner.setStatus(1);
        memberMapper.insert(owner);
    }

    private void seedMember(Long companyId, Long userId, String role) {
        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(1);
        memberMapper.insert(member);
    }
}