package com.robot.home.file;

import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.company.entity.CompanyClaim;
import com.robot.home.company.entity.CompanyMember;
import com.robot.home.company.entity.CompanyProfileChange;
import com.robot.home.company.mapper.CompanyClaimMapper;
import com.robot.home.company.mapper.CompanyProfileChangeMapper;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.security.LoginUser;
import com.robot.home.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 私有文件业务归属授权攻防测试
 *
 * 覆盖 FILE-01~FILE-15 安全场景：
 * FILE-01 未登录拒绝
 * FILE-02 普通用户无归属拒绝
 * FILE-03 跨企业访问拒绝
 * FILE-04 已退出企业成员拒绝
 * FILE-05 已撤销认证拒绝
 * FILE-06 猜测对象地址拒绝
 * FILE-07 修改bucket拒绝
 * FILE-08 修改private前缀拒绝
 * FILE-09 URL编码绕过拒绝
 * FILE-10 双重编码绕过拒绝
 * FILE-11 路径穿越拒绝
 * FILE-12 公开静态路径不可达私有文件
 * FILE-13 MinIO直接公开访问不可达私有文件
 * FILE-14 本地降级路径归属校验
 * FILE-15 管理员权限边界
 */
class PrivateFileAccessServiceTest {

    private CompanyClaimMapper claimMapper;
    private CompanyProfileChangeMapper changeMapper;
    private CompanyMemberService memberService;
    private PrivateFileAccessService service;

    private static final Long USER_A = 1001L;
    private static final Long USER_B = 2002L;
    private static final Long ADMIN_USER = 9999L;
    private static final Long COMPANY_A = 3001L;
    private static final Long COMPANY_B = 4002L;

    private static final String MINIO_PRIVATE_URL = "minio://robot-bucket/private/enterprise/abc123def456.jpg";
    private static final String LOCAL_PRIVATE_URL = "/api/files/private/enterprise/2026/10/22/xyz789.jpg";

    @BeforeEach
    void setUp() {
        claimMapper = mock(CompanyClaimMapper.class);
        changeMapper = mock(CompanyProfileChangeMapper.class);
        memberService = mock(CompanyMemberService.class);
        service = new PrivateFileAccessService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "companyClaimMapper", claimMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "companyProfileChangeMapper", changeMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "companyMemberService", memberService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private void loginUser(Long userId, String role) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setRole(role);
        UserContext.setUser(user);
    }

    private CompanyClaim claimOf(Long companyId, Long applicantUserId, String proofMaterials) {
        CompanyClaim claim = new CompanyClaim();
        claim.setCompanyId(companyId);
        claim.setApplicantUserId(applicantUserId);
        claim.setProofMaterials(proofMaterials);
        claim.setStatus("PENDING");
        return claim;
    }

    private CompanyMember memberOf(Long companyId, Long userId) {
        CompanyMember m = new CompanyMember();
        m.setCompanyId(companyId);
        m.setUserId(userId);
        m.setRole("OWNER");
        m.setStatus(1);
        return m;
    }

    // ==================== 正向授权测试 ====================

    @Test
    @DisplayName("FILE-15a: 管理员可访问任意私有文件（审核认证材料）")
    void adminCanAccessAnyPrivateFile() {
        loginUser(ADMIN_USER, Constants.ROLE_ADMIN);
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertDoesNotThrow(() -> service.checkAccess(ADMIN_USER, MINIO_PRIVATE_URL));
        // 管理员直接放行，不查业务记录
        verify(claimMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("认领申请人可访问自己上传的认证材料")
    void claimApplicantCanAccessOwnFiles() {
        loginUser(USER_A, "USER");
        String proofJson = "[\"/api/files/private?url=minio://robot-bucket/private/enterprise/abc123def456.jpg\"]";
        when(claimMapper.selectList(any()))
                .thenReturn(Collections.singletonList(claimOf(COMPANY_A, USER_A, proofJson)));
        assertDoesNotThrow(() -> service.checkAccess(USER_A, MINIO_PRIVATE_URL));
    }

    @Test
    @DisplayName("企业活跃成员可访问企业认领材料")
    void companyMemberCanAccessClaimFiles() {
        loginUser(USER_B, "USER");
        String proofJson = "[\"/api/files/private?url=minio://robot-bucket/private/enterprise/abc123def456.jpg\"]";
        when(claimMapper.selectList(any()))
                .thenReturn(Collections.singletonList(claimOf(COMPANY_A, USER_A, proofJson)));
        when(memberService.getActiveMember(USER_B, COMPANY_A)).thenReturn(memberOf(COMPANY_A, USER_B));
        assertDoesNotThrow(() -> service.checkAccess(USER_B, MINIO_PRIVATE_URL));
    }

    @Test
    @DisplayName("资料变更提交人可访问自己上传的文件")
    void profileChangeSubmitterCanAccessOwnFiles() {
        loginUser(USER_A, "USER");
        CompanyProfileChange change = new CompanyProfileChange();
        change.setCompanyId(COMPANY_A);
        change.setApplicantUserId(USER_A);
        change.setFieldName("logo");
        change.setNewValue("/api/files/private/enterprise/2026/10/22/xyz789.jpg");
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.singletonList(change));
        assertDoesNotThrow(() -> service.checkAccess(USER_A, LOCAL_PRIVATE_URL));
    }

    @Test
    @DisplayName("本地存储私有文件-企业成员授权通过")
    void localPrivateFileCompanyMemberAuthorized() {
        loginUser(USER_B, "USER");
        String proofJson = "[\"/api/files/private/enterprise/2026/10/22/xyz789.jpg\"]";
        when(claimMapper.selectList(any()))
                .thenReturn(Collections.singletonList(claimOf(COMPANY_A, USER_A, proofJson)));
        when(memberService.getActiveMember(USER_B, COMPANY_A)).thenReturn(memberOf(COMPANY_A, USER_B));
        assertDoesNotThrow(() -> service.checkAccess(USER_B, LOCAL_PRIVATE_URL));
    }

    // ==================== 攻防拒绝测试 ====================

    @Test
    @DisplayName("FILE-01: 未登录用户拒绝访问")
    void unauthenticatedUserDenied() {
        UserContext.clear();
        assertThrows(BusinessException.class, () -> service.checkAccess(null, MINIO_PRIVATE_URL));
    }

    @Test
    @DisplayName("FILE-02: 普通用户无业务归属拒绝")
    void regularUserWithoutOwnershipDenied() {
        loginUser(USER_B, "USER");
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, MINIO_PRIVATE_URL));
    }

    @Test
    @DisplayName("FILE-03: 企业A用户访问企业B的私有文件拒绝")
    void crossCompanyAccessDenied() {
        loginUser(USER_B, "USER");
        // 企业B的认领材料
        String proofJson = "[\"/api/files/private?url=minio://robot-bucket/private/enterprise/abc123def456.jpg\"]";
        when(claimMapper.selectList(any()))
                .thenReturn(Collections.singletonList(claimOf(COMPANY_A, USER_A, proofJson)));
        // USER_B 不是企业A成员
        when(memberService.getActiveMember(USER_B, COMPANY_A)).thenReturn(null);
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, MINIO_PRIVATE_URL));
    }

    @Test
    @DisplayName("FILE-04: 已退出企业成员（getActiveMember=null）拒绝")
    void removedCompanyMemberDenied() {
        loginUser(USER_B, "USER");
        String proofJson = "[\"/api/files/private?url=minio://robot-bucket/private/enterprise/abc123def456.jpg\"]";
        when(claimMapper.selectList(any()))
                .thenReturn(Collections.singletonList(claimOf(COMPANY_A, USER_A, proofJson)));
        // USER_B 曾是企业A成员但已退出
        when(memberService.getActiveMember(USER_B, COMPANY_A)).thenReturn(null);
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, MINIO_PRIVATE_URL));
    }

    @Test
    @DisplayName("FILE-05: 已撤销认证（claim REJECTED）的非申请人拒绝")
    void revokedCertificationNonApplicantDenied() {
        loginUser(USER_B, "USER");
        CompanyClaim rejectedClaim = claimOf(COMPANY_A, USER_A,
                "[\"/api/files/private?url=minio://robot-bucket/private/enterprise/abc123def456.jpg\"]");
        rejectedClaim.setStatus("REJECTED");
        when(claimMapper.selectList(any())).thenReturn(Collections.singletonList(rejectedClaim));
        when(memberService.getActiveMember(USER_B, COMPANY_A)).thenReturn(null);
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, MINIO_PRIVATE_URL));
    }

    @Test
    @DisplayName("FILE-06: 猜测对象地址（无匹配业务记录）拒绝")
    void guessedObjectUrlDenied() {
        loginUser(USER_B, "USER");
        String guessedUrl = "minio://robot-bucket/private/enterprise/guesseduuid999.jpg";
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, guessedUrl));
    }

    @Test
    @DisplayName("FILE-07: 修改bucket名称拒绝（无匹配业务记录）")
    void modifiedBucketDenied() {
        loginUser(USER_B, "USER");
        String wrongBucketUrl = "minio://evil-bucket/private/enterprise/abc123def456.jpg";
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, wrongBucketUrl));
    }

    @Test
    @DisplayName("FILE-08: 去掉private前缀的对象拒绝（无匹配业务记录）")
    void nonPrivatePrefixObjectDenied() {
        loginUser(USER_B, "USER");
        String publicObjectUrl = "minio://robot-bucket/enterprise/abc123def456.jpg";
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, publicObjectUrl));
    }

    @Test
    @DisplayName("FILE-09: URL编码绕过拒绝（%2F编码不会匹配业务记录）")
    void urlEncodedBypassDenied() {
        loginUser(USER_B, "USER");
        String encodedUrl = "minio://robot-bucket/private%2Fenterprise%2Fabc123def456.jpg";
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, encodedUrl));
    }

    @Test
    @DisplayName("FILE-10: 双重编码绕过拒绝")
    void doubleEncodedBypassDenied() {
        loginUser(USER_B, "USER");
        String doubleEncoded = "minio://robot-bucket/private%252Fenterprise%252Fabc123def456.jpg";
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, doubleEncoded));
    }

    @Test
    @DisplayName("FILE-11: 路径穿越URL拒绝（无匹配业务记录）")
    void pathTraversalUrlDenied() {
        loginUser(USER_B, "USER");
        String traversalUrl = "minio://robot-bucket/private/enterprise/../../etc/passwd";
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, traversalUrl));
    }

    @Test
    @DisplayName("FILE-12: 公开静态路径格式不可通过私有授权服务解析")
    void publicStaticPathNotParsable() {
        loginUser(USER_B, "USER");
        // /files/ 开头的公开路径不是私有文件URL格式
        String publicPath = "/files/enterprise/2026/10/22/somefile.jpg";
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        // extractFileSegment 返回 null → 拒绝
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, publicPath));
    }

    @Test
    @DisplayName("FILE-13: MinIO endpoint公开URL格式不可通过私有授权服务解析")
    void minioEndpointPublicUrlNotParsable() {
        loginUser(USER_B, "USER");
        String endpointUrl = "http://minio:9000/robot-bucket/enterprise/somefile.jpg";
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, endpointUrl));
    }

    @Test
    @DisplayName("FILE-14: 本地降级路径-无业务归属拒绝")
    void localFallbackPathWithoutOwnershipDenied() {
        loginUser(USER_B, "USER");
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, LOCAL_PRIVATE_URL));
    }

    @Test
    @DisplayName("FILE-15b: 非管理员用户不能访问无归属的历史文件")
    void nonAdminCannotAccessUnownedLegacyFile() {
        loginUser(USER_B, "USER");
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.checkAccess(USER_B, MINIO_PRIVATE_URL));
    }

    // ==================== extractFileSegment 单元测试 ====================

    @Test
    @DisplayName("extractFileSegment: MinIO URL正确提取对象路径")
    void extractSegmentFromMinioUrl() {
        assertEquals("private/enterprise/abc123def456.jpg",
                service.extractFileSegment("minio://robot-bucket/private/enterprise/abc123def456.jpg"));
    }

    @Test
    @DisplayName("extractFileSegment: 本地URL正确提取相对路径")
    void extractSegmentFromLocalUrl() {
        assertEquals("enterprise/2026/10/22/xyz789.jpg",
                service.extractFileSegment("/api/files/private/enterprise/2026/10/22/xyz789.jpg"));
    }

    @Test
    @DisplayName("extractFileSegment: 无法解析的格式返回null")
    void extractSegmentUnparsableReturnsNull() {
        assertNull(service.extractFileSegment("http://example.com/some/file.jpg"));
        assertNull(service.extractFileSegment(null));
        assertNull(service.extractFileSegment(""));
    }

    @Test
    @DisplayName("extractFileSegment: MinIO URL无bucket分隔符返回null")
    void extractSegmentMinioNoSlashReturnsNull() {
        assertNull(service.extractFileSegment("minio://nobucketseparator"));
    }

    // ========== LIKE通配符转义测试 ==========

    @Test
    @DisplayName("escapeLike: 百分号转义为字面量")
    void escapeLikePercent() {
        assertEquals("enterprise/100\\%", service.escapeLike("enterprise/100%"));
    }

    @Test
    @DisplayName("escapeLike: 下划线转义为字面量")
    void escapeLikeUnderscore() {
        assertEquals("enterprise/file\\_1.jpg", service.escapeLike("enterprise/file_1.jpg"));
    }

    @Test
    @DisplayName("escapeLike: 反斜杠转义")
    void escapeLikeBackslash() {
        assertEquals("enterprise/a\\\\b", service.escapeLike("enterprise/a\\b"));
    }

    @Test
    @DisplayName("escapeLike: null输入返回null")
    void escapeLikeNull() {
        assertNull(service.escapeLike(null));
    }

    @Test
    @DisplayName("escapeLike: 正常路径不受影响")
    void escapeLikeNormalPath() {
        assertEquals("private/enterprise/uuid.jpg", service.escapeLike("private/enterprise/uuid.jpg"));
    }

    @Test
    @DisplayName("FILE-16: 含%通配符的URL不能扩大匹配范围")
    void wildcardPercentUrlDenied() {
        loginUser(200L, "USER");
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        // 构造含%的URL，escapeLike后%作为字面量，不匹配任何记录→fail-closed拒绝
        assertThrows(BusinessException.class, () ->
                service.checkAccess(200L, "minio://bucket/private/%"));
    }

    @Test
    @DisplayName("FILE-17: 含_通配符的URL不能匹配相似文件名")
    void wildcardUnderscoreUrlDenied() {
        loginUser(200L, "USER");
        when(claimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(changeMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () ->
                service.checkAccess(200L, "minio://bucket/private/enterprise/file_1.jpg"));
    }
}