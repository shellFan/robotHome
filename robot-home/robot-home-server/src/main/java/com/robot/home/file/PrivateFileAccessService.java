package com.robot.home.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.company.entity.CompanyClaim;
import com.robot.home.company.entity.CompanyProfileChange;
import com.robot.home.company.mapper.CompanyClaimMapper;
import com.robot.home.company.mapper.CompanyProfileChangeMapper;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 私有文件业务归属授权服务
 *
 * 安全模型：用户身份 → 业务记录 → 企业归属 → 授权策略 → 文件对象 → 私有下载
 *
 * 仅凭 private/ 前缀和 bucket 校验不足以完成企业级授权。
 * 本服务通过反查业务记录（企业认领申请、企业资料变更）确定文件归属，
 * 验证当前登录用户对具体文件的访问权限：
 *
 * 1. 管理员（ADMIN角色）→ 允许（审核认证材料）
 * 2. 认领申请提交人 → 允许（申请人上传的文件）
 * 3. 认领申请所属企业的活跃成员 → 允许（企业自有文件）
 * 4. 资料变更提交人 → 允许（提交人上传的文件）
 * 5. 资料变更所属企业的活跃成员 → 允许
 * 6. 以上均不满足 → 拒绝（fail-closed）
 *
 * 兼容性：历史文件如未被任何业务记录引用，仅管理员可访问，不破坏管理员审核链路。
 */
@Service
public class PrivateFileAccessService {

    private static final Logger log = LoggerFactory.getLogger(PrivateFileAccessService.class);

    private static final String MINIO_SCHEME = "minio://";
    private static final String LOCAL_PRIVATE_PREFIX = "/api/files/private/";

    @Resource
    private CompanyClaimMapper companyClaimMapper;

    @Resource
    private CompanyProfileChangeMapper companyProfileChangeMapper;

    @Resource
    private CompanyMemberService companyMemberService;

    /**
     * 校验用户对私有文件的访问权限，无权限时抛出 BusinessException
     *
     * @param userId  当前登录用户ID
     * @param fileUrl 私有文件URL（minio://bucket/object 或 /api/files/private/...）
     */
    public void checkAccess(Long userId, String fileUrl) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }

        // 1. 管理员始终允许（审核认证材料、资料变更）
        if (isAdmin()) {
            return;
        }

        // 2. 提取文件唯一路径段用于业务记录反查（转义LIKE通配符防注入）
        String fileSegment = extractFileSegment(fileUrl);
        if (fileSegment == null || fileSegment.isEmpty()) {
            log.warn("私有文件访问拒绝: 无法解析文件路径段 userId={}, url={}", userId, fileUrl);
            throw new BusinessException("无权访问该文件");
        }
        String escapedSegment = escapeLike(fileSegment);

        // 3. 反查企业认领申请（proofMaterials JSON数组包含文件URL）
        List<CompanyClaim> claims = companyClaimMapper.selectList(
                Wrappers.<CompanyClaim>lambdaQuery()
                        .like(CompanyClaim::getProofMaterials, escapedSegment));
        for (CompanyClaim claim : claims) {
            // 申请人可访问自己上传的认证材料
            if (userId.equals(claim.getApplicantUserId())) {
                return;
            }
            // 认领所属企业的活跃成员可访问（企业自有文件）
            if (claim.getCompanyId() != null
                    && companyMemberService.getActiveMember(userId, claim.getCompanyId()) != null) {
                return;
            }
        }

        // 4. 反查企业资料变更（newValue 可能包含文件URL，如 logo 变更）
        List<CompanyProfileChange> changes = companyProfileChangeMapper.selectList(
                Wrappers.<CompanyProfileChange>lambdaQuery()
                        .like(CompanyProfileChange::getNewValue, escapedSegment));
        for (CompanyProfileChange change : changes) {
            // 提交人可访问自己上传的文件
            if (userId.equals(change.getApplicantUserId())) {
                return;
            }
            // 变更所属企业的活跃成员可访问
            if (change.getCompanyId() != null
                    && companyMemberService.getActiveMember(userId, change.getCompanyId()) != null) {
                return;
            }
        }

        // 5. fail-closed：未匹配任何业务记录归属
        log.warn("私有文件访问拒绝(无业务归属): userId={}, fileSegment={}", userId, fileSegment);
        throw new BusinessException("无权访问该文件");
    }

    /**
     * 从私有文件URL中提取唯一路径段，用于业务记录 LIKE 反查
     *
     * MinIO: minio://bucket/private/enterprise/uuid.jpg → private/enterprise/uuid.jpg
     * 本地:  /api/files/private/enterprise/2026/10/22/uuid.jpg → enterprise/2026/10/22/uuid.jpg
     */
    String extractFileSegment(String fileUrl) {
        if (fileUrl == null) {
            return null;
        }
        // MinIO URL: 去掉 scheme 和 bucket，保留对象路径
        if (fileUrl.startsWith(MINIO_SCHEME)) {
            String path = fileUrl.substring(MINIO_SCHEME.length());
            int slash = path.indexOf('/');
            return slash > 0 ? path.substring(slash + 1) : null;
        }
        // 本地私有 URL: 去掉 /api/files/private/ 前缀
        int idx = fileUrl.indexOf(LOCAL_PRIVATE_PREFIX);
        if (idx >= 0) {
            return fileUrl.substring(idx + LOCAL_PRIVATE_PREFIX.length());
        }
        return null;
    }

    /**
     * 转义LIKE通配符（%、_、\），防止用户构造含通配符的URL扩大匹配范围
     *
     * MySQL LIKE默认转义符为\，转义后%和_作为字面量匹配
     */
    String escapeLike(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(UserContext.getRole());
    }
}