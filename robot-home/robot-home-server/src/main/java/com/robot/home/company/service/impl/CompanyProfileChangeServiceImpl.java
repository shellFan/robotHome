package com.robot.home.company.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.company.entity.Company;
import com.robot.home.company.entity.CompanyProfileChange;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.mapper.CompanyProfileChangeMapper;
import com.robot.home.company.service.CompanyProfileChangeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 企业资料变更服务实现
 */
@Slf4j
@Service
public class CompanyProfileChangeServiceImpl implements CompanyProfileChangeService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    /**
     * 高风险字段: 修改需审核(影响企业身份/联系方式/品牌形象)
     */
    private static final Set<String> HIGH_RISK_FIELDS = new HashSet<>(Arrays.asList(
            "logo", "website", "contactPhone", "contactEmail", "address",
            "name", "shortName", "companyNameEn"
    ));

    /**
     * 允许企业修改的字段白名单
     */
    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
            "logo", "website", "contactPhone", "contactEmail", "address",
            "name", "shortName", "companyNameEn",
            "intro", "seoTitle", "seoKeywords", "seoDescription", "detail",
            "foundYear", "region", "province", "city", "businessScope", "tags"
    ));

    private static final Map<String, String> FIELD_LABELS = new HashMap<>();

    static {
        FIELD_LABELS.put("logo", "企业Logo");
        FIELD_LABELS.put("website", "官网");
        FIELD_LABELS.put("contactPhone", "联系电话");
        FIELD_LABELS.put("contactEmail", "联系邮箱");
        FIELD_LABELS.put("address", "办公地址");
        FIELD_LABELS.put("name", "企业名称");
        FIELD_LABELS.put("shortName", "企业简称");
        FIELD_LABELS.put("companyNameEn", "企业英文名");
        FIELD_LABELS.put("intro", "企业简介");
        FIELD_LABELS.put("seoTitle", "SEO标题");
        FIELD_LABELS.put("seoKeywords", "SEO关键词");
        FIELD_LABELS.put("seoDescription", "SEO描述");
        FIELD_LABELS.put("detail", "详细介绍");
        FIELD_LABELS.put("foundYear", "成立年份");
        FIELD_LABELS.put("region", "所在区域");
        FIELD_LABELS.put("province", "省份");
        FIELD_LABELS.put("city", "城市");
        FIELD_LABELS.put("businessScope", "业务范围");
        FIELD_LABELS.put("tags", "标签");
    }

    @Resource
    private CompanyProfileChangeMapper changeMapper;
    @Resource
    private CompanyMapper companyMapper;

    @Override
    public boolean isHighRiskField(String fieldName) {
        return fieldName != null && HIGH_RISK_FIELDS.contains(fieldName);
    }

    @Override
    @Transactional
    public CompanyProfileChange submitChange(Long companyId, Long applicantUserId, String applicantName,
                                             String fieldName, String fieldLabel,
                                             String oldValue, String newValue, String changeReason) {
        requireVerifiedCompany(companyId);
        validateField(fieldName);

        // 校验无同字段待审核申请
        Long pendingCount = changeMapper.selectCount(Wrappers.<CompanyProfileChange>lambdaQuery()
                .eq(CompanyProfileChange::getCompanyId, companyId)
                .eq(CompanyProfileChange::getFieldName, fieldName)
                .eq(CompanyProfileChange::getStatus, STATUS_PENDING));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("该字段已有待审核的变更申请");
        }

        CompanyProfileChange change = new CompanyProfileChange();
        change.setCompanyId(companyId);
        change.setApplicantUserId(applicantUserId);
        change.setApplicantName(applicantName);
        change.setChangeType("PROFILE");
        change.setFieldName(fieldName);
        change.setFieldLabel(fieldLabel != null ? fieldLabel : FIELD_LABELS.getOrDefault(fieldName, fieldName));
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        change.setChangeReason(changeReason);
        change.setStatus(STATUS_PENDING);
        changeMapper.insert(change);

        log.info("企业资料变更申请: companyId={}, field={}, applicantUserId={}",
                companyId, fieldName, applicantUserId);
        return change;
    }

    @Override
    @Transactional
    public void directUpdate(Long companyId, Long applicantUserId, String fieldName, String newValue) {
        requireVerifiedCompany(companyId);
        validateField(fieldName);

        if (isHighRiskField(fieldName)) {
            throw new BusinessException("字段[" + fieldName + "]为高风险字段, 需提交审核");
        }

        Company update = new Company();
        update.setId(companyId);
        setFieldValue(update, fieldName, newValue);
        companyMapper.updateById(update);

        log.info("企业资料直接更新: companyId={}, field={}, operatorUserId={}",
                companyId, fieldName, applicantUserId);
    }

    @Override
    public PageResult<CompanyProfileChange> myChanges(Long userId, Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
        Page<CompanyProfileChange> page = new Page<>(pn, ps);
        Page<CompanyProfileChange> result = changeMapper.selectPage(page,
                Wrappers.<CompanyProfileChange>lambdaQuery()
                        .eq(CompanyProfileChange::getApplicantUserId, userId)
                        .orderByDesc(CompanyProfileChange::getCreateTime));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<CompanyProfileChange> listByStatus(String status, Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
        Page<CompanyProfileChange> page = new Page<>(pn, ps);
        Page<CompanyProfileChange> result;
        if (status == null || status.isEmpty()) {
            result = changeMapper.selectPage(page,
                    Wrappers.<CompanyProfileChange>lambdaQuery()
                            .orderByDesc(CompanyProfileChange::getCreateTime));
        } else {
            result = changeMapper.selectPage(page,
                    Wrappers.<CompanyProfileChange>lambdaQuery()
                            .eq(CompanyProfileChange::getStatus, status)
                            .orderByDesc(CompanyProfileChange::getCreateTime));
        }
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public CompanyProfileChange reviewChange(Long changeId, Long reviewerUserId, String reviewerName,
                                             boolean approved, String reviewComment) {
        CompanyProfileChange change = changeMapper.selectById(changeId);
        if (change == null) {
            throw new BusinessException("变更申请不存在");
        }
        if (!STATUS_PENDING.equals(change.getStatus())) {
            throw new BusinessException("该申请已审核，不可重复操作");
        }

        LocalDateTime reviewTime = LocalDateTime.now();
        String nextStatus = approved ? STATUS_APPROVED : STATUS_REJECTED;
        int updated = changeMapper.update(null, Wrappers.<CompanyProfileChange>lambdaUpdate()
                .eq(CompanyProfileChange::getId, changeId)
                .eq(CompanyProfileChange::getStatus, STATUS_PENDING)
                .set(CompanyProfileChange::getStatus, nextStatus)
                .set(CompanyProfileChange::getReviewerUserId, reviewerUserId)
                .set(CompanyProfileChange::getReviewerName, reviewerName)
                .set(CompanyProfileChange::getReviewTime, reviewTime)
                .set(CompanyProfileChange::getReviewComment, reviewComment)
                .set(CompanyProfileChange::getUpdateTime, reviewTime));
        if (updated != 1) {
            throw new BusinessException("申请状态已变化，请刷新后重试");
        }
        change.setStatus(nextStatus);
        change.setReviewerUserId(reviewerUserId);
        change.setReviewerName(reviewerName);
        change.setReviewTime(reviewTime);
        change.setReviewComment(reviewComment);

        if (approved) {

            // 将新值应用到企业
            Company update = new Company();
            update.setId(change.getCompanyId());
            setFieldValue(update, change.getFieldName(), change.getNewValue());
            if (companyMapper.updateById(update) != 1) {
                throw new BusinessException("企业不存在，无法应用资料变更");
            }

            log.info("企业资料变更通过: changeId={}, companyId={}, field={}",
                    changeId, change.getCompanyId(), change.getFieldName());
        } else {
            log.info("企业资料变更拒绝: changeId={}, companyId={}", changeId, change.getCompanyId());
        }

        return change;
    }

    // ==================== 私有方法 ====================

    private void validateField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new BusinessException("字段名不能为空");
        }
        if (!ALLOWED_FIELDS.contains(fieldName)) {
            throw new BusinessException("不允许修改字段: " + fieldName);
        }
    }

    private void requireVerifiedCompany(Long companyId) {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("企业不存在");
        }
        if (!"VERIFIED".equals(company.getVerificationStatus())) {
            throw new BusinessException("企业认证通过后才能维护企业资料");
        }
    }

    /**
     * 安全设置字段值(反射不可用, 用switch)
     */
    private void setFieldValue(Company company, String fieldName, String newValue) {
        if (newValue != null && newValue.length() > 2000) {
            newValue = newValue.substring(0, 2000);
        }
        switch (fieldName) {
            case "logo": company.setLogo(newValue); break;
            case "website": company.setWebsite(newValue); break;
            case "contactPhone": company.setContactPhone(newValue); break;
            case "contactEmail": company.setContactEmail(newValue); break;
            case "address": company.setAddress(newValue); break;
            case "name": company.setName(newValue); break;
            case "shortName": company.setShortName(newValue); break;
            case "companyNameEn": company.setCompanyNameEn(newValue); break;
            case "intro": company.setIntro(newValue); break;
            case "seoTitle": company.setSeoTitle(newValue); break;
            case "seoKeywords": company.setSeoKeywords(newValue); break;
            case "seoDescription": company.setSeoDescription(newValue); break;
            case "detail": company.setDetail(newValue); break;
            case "foundYear":
                if (newValue != null && !newValue.isEmpty()) {
                    try {
                        company.setFoundYear(Integer.parseInt(newValue));
                    } catch (NumberFormatException e) {
                        throw new BusinessException("成立年份格式错误");
                    }
                }
                break;
            case "region": company.setRegion(newValue); break;
            case "province": company.setProvince(newValue); break;
            case "city": company.setCity(newValue); break;
            case "businessScope": company.setBusinessScope(newValue); break;
            case "tags": company.setTags(newValue); break;
            default:
                throw new BusinessException("不支持的字段: " + fieldName);
        }
    }
}
