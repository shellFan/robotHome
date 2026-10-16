package com.robot.home.procurement.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.procurement.service.LeadRecommendationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 商机推荐接口（企业端）
 * <p>
 * 配额：LEAD_VIEW查看详情 / LEAD_RESPONSE接受商机
 */
@RestController
@RequestMapping("/api/lead-recommendations")
public class LeadRecommendationController {

    @Resource
    private LeadRecommendationService leadRecommendationService;
    @Resource
    private CompanyMemberService companyMemberService;

    /**
     * 生成商机推荐
     */
    @PostMapping("/generate")
    public Result<?> generate(
            @RequestParam Long companyId,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(leadRecommendationService.generateRecommendations(companyId, limit));
    }

    /**
     * 推荐列表
     */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(leadRecommendationService.listRecommendations(companyId, status, pageNum, pageSize));
    }

    /**
     * 查看商机详情（消耗LEAD_VIEW配额）
     */
    @GetMapping("/{recommendationId}")
    public Result<Map<String, Object>> viewDetail(
            @PathVariable Long recommendationId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(leadRecommendationService.viewLeadDetail(recommendationId, companyId));
    }

    /**
     * 接受商机（消耗LEAD_RESPONSE配额）
     */
    @PostMapping("/{recommendationId}/accept")
    public Result<Void> accept(
            @PathVariable Long recommendationId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        leadRecommendationService.acceptLead(recommendationId, companyId);
        return Result.success(null);
    }

    /**
     * 拒绝商机
     */
    @PostMapping("/{recommendationId}/reject")
    public Result<Void> reject(
            @PathVariable Long recommendationId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        leadRecommendationService.rejectLead(recommendationId, companyId);
        return Result.success(null);
    }
}