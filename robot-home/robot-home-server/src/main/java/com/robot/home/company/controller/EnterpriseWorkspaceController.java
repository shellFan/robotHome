package com.robot.home.company.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.entity.CompanyMember;
import com.robot.home.company.entity.CompanyProfileChange;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.company.service.CompanyProfileChangeService;
import com.robot.home.security.LoginUser;
import com.robot.home.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 企业工作台 Controller
 * <p>
 * 已认证企业(VERIFIED)的成员可访问, 通过CompanyMemberService校验成员身份
 */
@Slf4j
@RestController
@RequestMapping("/api/workspace")
public class EnterpriseWorkspaceController {

    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private CompanyMemberService companyMemberService;
    @Resource
    private CompanyProfileChangeService profileChangeService;

    /**
     * 我的企业工作台首页(企业信息+成员角色)
     */
    @GetMapping("/company")
    public Result<Map<String, Object>> myCompany() {
        Long userId = SecurityUtils.requireUserId();
        Long companyId = companyMemberService.getFirstCompanyId(userId);
        if (companyId == null) {
            return Result.success(null);
        }
        Company company = companyMapper.selectById(companyId);
        CompanyMember member = companyMemberService.getActiveMember(userId, companyId);

        Map<String, Object> data = new HashMap<>();
        data.put("company", company);
        data.put("myRole", member != null ? member.getRole() : null);
        data.put("isVerified", company != null && "VERIFIED".equals(company.getVerificationStatus()));
        return Result.success(data);
    }

    /**
     * 提交资料变更申请(高风险字段: logo/website/contact等)
     */
    @PostMapping("/profile-change/submit")
    public Result<CompanyProfileChange> submitProfileChange(
            @RequestParam Long companyId,
            @RequestParam String fieldName,
            @RequestParam String newValue,
            @RequestParam(required = false) String changeReason) {
        Long userId = SecurityUtils.requireUserId();
        // 安全校验: 必须是该企业的活跃成员
        companyMemberService.requireActiveMember(userId, companyId);

        LoginUser loginUser = UserContext.getUser();
        String applicantName = loginUser != null ? loginUser.getUsername() : String.valueOf(userId);

        // 获取原值
        Company company = companyMapper.selectById(companyId);
        String oldValue = getFieldValue(company, fieldName);

        CompanyProfileChange change = profileChangeService.submitChange(
                companyId, userId, applicantName, fieldName, null,
                oldValue, newValue, changeReason);
        return Result.success(change);
    }

    /**
     * 直接更新低风险字段(intro/seo等)
     */
    @PostMapping("/profile-update")
    public Result<Void> directUpdate(
            @RequestParam Long companyId,
            @RequestParam String fieldName,
            @RequestParam String newValue) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        profileChangeService.directUpdate(companyId, userId, fieldName, newValue);
        return Result.success();
    }

    /**
     * 我的变更申请列表
     */
    @GetMapping("/profile-changes")
    public Result<PageResult<CompanyProfileChange>> myProfileChanges(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(profileChangeService.myChanges(userId, pageNum, pageSize));
    }

    /**
     * 查询字段是否高风险(前端用于决定走审核还是直接更新)
     */
    @GetMapping("/field-risk")
    public Result<Map<String, Object>> fieldRisk(@RequestParam String fieldName) {
        Map<String, Object> data = new HashMap<>();
        data.put("fieldName", fieldName);
        data.put("highRisk", profileChangeService.isHighRiskField(fieldName));
        return Result.success(data);
    }

    // ==================== 私有方法 ====================

    private String getFieldValue(Company company, String fieldName) {
        if (company == null || fieldName == null) {
            return null;
        }
        switch (fieldName) {
            case "logo": return company.getLogo();
            case "website": return company.getWebsite();
            case "contactPhone": return company.getContactPhone();
            case "contactEmail": return company.getContactEmail();
            case "address": return company.getAddress();
            case "name": return company.getName();
            case "shortName": return company.getShortName();
            case "companyNameEn": return company.getCompanyNameEn();
            case "intro": return company.getIntro();
            case "seoTitle": return company.getSeoTitle();
            case "seoKeywords": return company.getSeoKeywords();
            case "seoDescription": return company.getSeoDescription();
            case "detail": return company.getDetail();
            case "foundYear": return company.getFoundYear() != null ? String.valueOf(company.getFoundYear()) : null;
            case "region": return company.getRegion();
            case "province": return company.getProvince();
            case "city": return company.getCity();
            case "businessScope": return company.getBusinessScope();
            case "tags": return company.getTags();
            default: return null;
        }
    }
}