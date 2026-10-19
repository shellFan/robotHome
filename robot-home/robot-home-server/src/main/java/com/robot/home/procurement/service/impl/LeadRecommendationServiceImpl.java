package com.robot.home.procurement.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.commercial.entity.EnterpriseEntitlement;
import com.robot.home.commercial.service.EntitlementService;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.procurement.entity.LeadRecommendation;
import com.robot.home.procurement.mapper.LeadRecommendationMapper;
import com.robot.home.procurement.service.LeadRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商机推荐服务实现
 * <p>
 * 推荐算法（评分维度）：
 * 1. 分类匹配（inquiry.category vs company.businessScope/tags）：+40分
 * 2. 地区匹配（inquiry.region vs company.province/city/region）：+30分
 * 3. 企业能力（productCount/brandCount）：+10~20分
 * 4. 线索优先级（leadPriority）：+0~10分
 * <p>
 * 配额集成：
 * - LEAD_VIEW：查看商机详情消耗1次
 * - LEAD_RESPONSE：接受商机消耗1次
 */
@Slf4j
@Service
public class LeadRecommendationServiceImpl implements LeadRecommendationService {

    @Resource
    private LeadRecommendationMapper recommendationMapper;
    @Resource
    private InquiryMapper inquiryMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private EntitlementService entitlementService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<LeadRecommendation> generateRecommendations(Long companyId, int limit) {
        if (limit <= 0) limit = 10;
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("企业不存在");
        }
        // 查询待处理线索（status=1 待处理，且未被此企业拒绝过）
        List<Inquiry> inquiries = inquiryMapper.selectList(
                Wrappers.<Inquiry>lambdaQuery()
                        .eq(Inquiry::getStatus, 1)
                        .orderByDesc(Inquiry::getLeadPriority)
                        .orderByDesc(Inquiry::getId)
                        .last("LIMIT " + (limit * 3)));
        List<LeadRecommendation> created = new ArrayList<>();
        for (Inquiry inquiry : inquiries) {
            if (created.size() >= limit) break;
            // 检查是否已推荐过
            long exists = recommendationMapper.selectCount(
                    Wrappers.<LeadRecommendation>lambdaQuery()
                            .eq(LeadRecommendation::getCompanyId, companyId)
                            .eq(LeadRecommendation::getProcurementId, inquiry.getId()));
            if (exists > 0) continue;
            // 计算匹配分
            int score = calculateMatchScore(company, inquiry);
            if (score < 30) continue; // 最低匹配阈值
            String reason = buildMatchReason(score, company, inquiry);
            LeadRecommendation rec = new LeadRecommendation();
            rec.setCompanyId(companyId);
            rec.setProcurementId(inquiry.getId());
            rec.setScore(score);
            rec.setReason(reason);
            rec.setStatus("PENDING");
            rec.setRecommendedTime(LocalDateTime.now());
            try {
                recommendationMapper.insert(rec);
                created.add(rec);
            } catch (DuplicateKeyException e) {
                // uk_company_proc防重复
                log.debug("商机推荐重复跳过: companyId={}, procurementId={}", companyId, inquiry.getId());
            }
        }
        log.info("商机推荐生成: companyId={}, created={}", companyId, created.size());
        return created;
    }

    @Override
    public PageResult<Map<String, Object>> listRecommendations(Long companyId, String status, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<LeadRecommendation> page = new Page<>(pn, ps);
        Page<LeadRecommendation> result = recommendationMapper.selectPage(page,
                Wrappers.<LeadRecommendation>lambdaQuery()
                        .eq(LeadRecommendation::getCompanyId, companyId)
                        .eq(StrUtil.isNotBlank(status), LeadRecommendation::getStatus, status)
                        .orderByDesc(LeadRecommendation::getScore)
                        .orderByDesc(LeadRecommendation::getId));
        // 检查LEAD_VIEW配额
        EnterpriseEntitlement leadViewEnt = entitlementService.checkEntitlement(companyId, "LEAD_VIEW");
        boolean canViewDetail = leadViewEnt != null && entitlementService.hasQuota(leadViewEnt);
        List<Map<String, Object>> records = new ArrayList<>();
        for (LeadRecommendation rec : result.getRecords()) {
            records.add(buildRecommendationView(rec, canViewDetail));
        }
        return PageResult.of(pn, ps, result.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> viewLeadDetail(Long recommendationId, Long companyId) {
        LeadRecommendation rec = getAndCheckOwner(recommendationId, companyId);
        Inquiry inquiry = inquiryMapper.selectById(rec.getProcurementId());
        if (inquiry == null) {
            throw new BusinessException("商机不存在");
        }
        // 检查配额
        EnterpriseEntitlement leadViewEnt = entitlementService.checkEntitlement(companyId, "LEAD_VIEW");
        boolean hasQuota = leadViewEnt != null && entitlementService.hasQuota(leadViewEnt);
        Map<String, Object> detail = new HashMap<>();
        detail.put("recommendationId", rec.getId());
        detail.put("score", rec.getScore());
        detail.put("reason", rec.getReason());
        detail.put("status", rec.getStatus());
        // 商机基本信息（始终可见）
        detail.put("robotName", inquiry.getRobotName());
        detail.put("category", inquiry.getCategory());
        detail.put("usageScene", inquiry.getUsageScene());
        detail.put("quantity", inquiry.getQuantity());
        detail.put("budget", inquiry.getBudget());
        detail.put("region", inquiry.getRegion());
        detail.put("procurementScene", inquiry.getProcurementScene());
        detail.put("requirementType", inquiry.getRequirementType());
        detail.put("leadPriority", inquiry.getLeadPriority());
        detail.put("needDemo", inquiry.getNeedDemo());
        detail.put("needSolution", inquiry.getNeedSolution());
        detail.put("remark", inquiry.getRemark());
        detail.put("purchaseTime", inquiry.getPurchaseTime());
        detail.put("createTime", inquiry.getCreateTime());
        // 联系方式（需配额）
        if (hasQuota) {
            // 消耗配额
            try {
                entitlementService.consumeQuota(companyId, "LEAD_VIEW",
                        "LEAD_DETAIL", rec.getProcurementId());
                detail.put("contactName", inquiry.getContactName());
                detail.put("phone", inquiry.getPhone());
                detail.put("email", inquiry.getEmail());
                detail.put("companyName", inquiry.getCompanyName());
                detail.put("contactVisible", true);
            } catch (BusinessException e) {
                detail.put("contactVisible", false);
                detail.put("quotaMessage", "LEAD_VIEW配额已用完，升级套餐获取更多商机查看次数");
            }
        } else {
            detail.put("contactVisible", false);
            detail.put("quotaMessage", "LEAD_VIEW配额已用完，升级套餐获取更多商机查看次数");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptLead(Long recommendationId, Long companyId) {
        LeadRecommendation rec = getAndCheckOwner(recommendationId, companyId);
        if (!"PENDING".equals(rec.getStatus())) {
            throw new BusinessException("当前状态不支持接受");
        }
        // 消耗LEAD_RESPONSE配额
        entitlementService.consumeQuota(companyId, "LEAD_RESPONSE",
                "LEAD_ACCEPT", recommendationId);
        // CAS更新状态
        int updated = recommendationMapper.update(null,
                Wrappers.<LeadRecommendation>lambdaUpdate()
                        .set(LeadRecommendation::getStatus, "ACCEPTED")
                        .eq(LeadRecommendation::getId, recommendationId)
                        .eq(LeadRecommendation::getStatus, "PENDING"));
        if (updated == 0) {
            // 竞争失败，释放配额
            entitlementService.releaseQuota(companyId, "LEAD_RESPONSE",
                    "LEAD_ACCEPT", recommendationId);
            throw new BusinessException("接受失败，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectLead(Long recommendationId, Long companyId) {
        LeadRecommendation rec = getAndCheckOwner(recommendationId, companyId);
        if (!"PENDING".equals(rec.getStatus())) {
            throw new BusinessException("当前状态不支持拒绝");
        }
        recommendationMapper.update(null,
                Wrappers.<LeadRecommendation>lambdaUpdate()
                        .set(LeadRecommendation::getStatus, "REJECTED")
                        .eq(LeadRecommendation::getId, recommendationId)
                        .eq(LeadRecommendation::getStatus, "PENDING"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchGenerateForAllCompanies(int perCompanyLimit) {
        if (perCompanyLimit <= 0) perCompanyLimit = 5;
        // 查所有认证企业
        List<Company> companies = companyMapper.selectList(
                Wrappers.<Company>lambdaQuery()
                        .eq(Company::getVerificationStatus, "VERIFIED")
                        .eq(Company::getStatus, 1));
        int total = 0;
        for (Company company : companies) {
            try {
                List<LeadRecommendation> recs = generateRecommendations(company.getId(), perCompanyLimit);
                total += recs.size();
            } catch (Exception e) {
                log.warn("企业商机推荐生成失败: companyId={}", company.getId(), e);
            }
        }
        log.info("批量商机推荐生成: companies={}, total={}", companies.size(), total);
        return total;
    }

    @Override
    public PageResult<LeadRecommendation> adminListRecommendations(String status, Long companyId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<LeadRecommendation> page = new Page<>(pn, ps);
        Page<LeadRecommendation> result = recommendationMapper.selectPage(page,
                Wrappers.<LeadRecommendation>lambdaQuery()
                        .eq(StrUtil.isNotBlank(status), LeadRecommendation::getStatus, status)
                        .eq(companyId != null, LeadRecommendation::getCompanyId, companyId)
                        .orderByDesc(LeadRecommendation::getId));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    // ==================== 私有方法 ====================

    private LeadRecommendation getAndCheckOwner(Long recommendationId, Long companyId) {
        LeadRecommendation rec = recommendationMapper.selectById(recommendationId);
        if (rec == null) {
            throw new BusinessException("商机推荐不存在");
        }
        if (!rec.getCompanyId().equals(companyId)) {
            throw new BusinessException("无权操作此商机推荐");
        }
        return rec;
    }

    /**
     * 计算匹配分
     */
    private int calculateMatchScore(Company company, Inquiry inquiry) {
        int score = 0;
        // 1. 分类匹配
        String category = inquiry.getCategory();
        if (StrUtil.isNotBlank(category)) {
            String scope = StrUtil.blankToDefault(company.getBusinessScope(), "");
            String tags = StrUtil.blankToDefault(company.getTags(), "");
            if (scope.contains(category) || tags.contains(category)) {
                score += 40;
            }
        }
        // 2. 地区匹配
        String region = inquiry.getRegion();
        if (StrUtil.isNotBlank(region)) {
            String companyRegion = StrUtil.blankToDefault(company.getRegion(), "")
                    + StrUtil.blankToDefault(company.getProvince(), "")
                    + StrUtil.blankToDefault(company.getCity(), "");
            if (companyRegion.contains(region) || region.contains(StrUtil.blankToDefault(company.getCity(), ""))) {
                score += 30;
            }
        }
        // 3. 企业能力
        int productCount = company.getProductCount() != null ? company.getProductCount() : 0;
        int brandCount = company.getBrandCount() != null ? company.getBrandCount() : 0;
        if (productCount >= 10) score += 15;
        else if (productCount >= 5) score += 10;
        else if (productCount > 0) score += 5;
        if (brandCount >= 3) score += 5;
        // 4. 线索优先级加分
        int priority = inquiry.getLeadPriority() != null ? inquiry.getLeadPriority() : 0;
        score += priority * 5;
        return score;
    }

    private String buildMatchReason(int score, Company company, Inquiry inquiry) {
        StringBuilder reason = new StringBuilder();
        if (StrUtil.isNotBlank(inquiry.getCategory())
                && (StrUtil.blankToDefault(company.getBusinessScope(), "").contains(inquiry.getCategory())
                || StrUtil.blankToDefault(company.getTags(), "").contains(inquiry.getCategory()))) {
            reason.append("分类匹配; ");
        }
        if (StrUtil.isNotBlank(inquiry.getRegion())
                && (StrUtil.blankToDefault(company.getRegion(), "").contains(inquiry.getRegion())
                || StrUtil.blankToDefault(company.getCity(), "").contains(inquiry.getRegion()))) {
            reason.append("地区匹配; ");
        }
        if (reason.length() == 0) {
            reason.append("综合推荐");
        }
        return reason.toString() + "匹配分:" + score;
    }

    private Map<String, Object> buildRecommendationView(LeadRecommendation rec, boolean canViewDetail) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", rec.getId());
        view.put("procurementId", rec.getProcurementId());
        view.put("score", rec.getScore());
        view.put("reason", rec.getReason());
        view.put("status", rec.getStatus());
        view.put("recommendedTime", rec.getRecommendedTime());
        // 补充商机摘要
        Inquiry inquiry = inquiryMapper.selectById(rec.getProcurementId());
        if (inquiry != null) {
            view.put("robotName", inquiry.getRobotName());
            view.put("category", inquiry.getCategory());
            view.put("region", inquiry.getRegion());
            view.put("quantity", inquiry.getQuantity());
            view.put("budget", inquiry.getBudget());
            view.put("leadPriority", inquiry.getLeadPriority());
            if (!canViewDetail) {
                view.put("contactHidden", true);
            }
        }
        return view;
    }
}