package com.robot.home.procurement.service;

import com.robot.home.common.PageResult;
import com.robot.home.procurement.entity.LeadRecommendation;

import java.util.List;
import java.util.Map;

/**
 * 商机推荐服务
 * <p>
 * 推荐算法：分类匹配+企业能力+地区+预算
 * 配额集成：LEAD_VIEW（查看商机）/ LEAD_RESPONSE（响应商机）
 */
public interface LeadRecommendationService {

    /**
     * 为企业生成商机推荐（基于未分配的询价线索匹配）
     *
     * @param companyId 企业ID
     * @param limit 最大推荐数
     * @return 推荐列表
     */
    List<LeadRecommendation> generateRecommendations(Long companyId, int limit);

    /**
     * 获取企业推荐列表（含商机详情，需LEAD_VIEW配额）
     *
     * @param companyId 企业ID
     * @param status 状态过滤
     * @param pageNum 页码
     * @param pageSize 每页
     * @return 推荐分页（商机详情仅在配额足够时返回联系方式）
     */
    PageResult<Map<String, Object>> listRecommendations(Long companyId, String status, Integer pageNum, Integer pageSize);

    /**
     * 查看商机详情（消耗LEAD_VIEW配额）
     *
     * @param recommendationId 推荐ID
     * @param companyId 企业ID
     * @return 商机详情（配额不足时隐藏联系方式）
     */
    Map<String, Object> viewLeadDetail(Long recommendationId, Long companyId);

    /**
     * 接受商机（消耗LEAD_RESPONSE配额，标记ACCEPTED）
     */
    void acceptLead(Long recommendationId, Long companyId);

    /**
     * 拒绝商机（标记REJECTED，不消耗配额）
     */
    void rejectLead(Long recommendationId, Long companyId);

    /**
     * Admin: 为所有认证企业批量生成推荐
     */
    int batchGenerateForAllCompanies(int perCompanyLimit);

    /**
     * Admin: 分页查询推荐记录
     */
    PageResult<LeadRecommendation> adminListRecommendations(String status, Long companyId, Integer pageNum, Integer pageSize);
}