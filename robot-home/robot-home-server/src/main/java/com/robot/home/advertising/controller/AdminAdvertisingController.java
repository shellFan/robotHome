package com.robot.home.advertising.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.security.RequirePermission;
import com.robot.home.advertising.entity.AdvertisingCampaign;
import com.robot.home.advertising.entity.AdvertisingSlot;
import com.robot.home.advertising.service.AdvertisingService;
import com.robot.home.procurement.service.LeadRecommendationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Admin广告运营接口
 * <p>
 * 广告位管理+审核+商机推荐管理
 */
@RestController
@RequestMapping("/api/admin/advertising")
public class AdminAdvertisingController {

    @Resource
    private AdvertisingService advertisingService;
    @Resource
    private LeadRecommendationService leadRecommendationService;

    // ==================== 广告位管理 ====================

    @GetMapping("/slots")
    @RequirePermission("advertising:slot:manage")
    public Result<?> listAllSlots() {
        return Result.success(advertisingService.listSlots());
    }

    @PostMapping("/slots")
    @RequirePermission("advertising:slot:manage")
    public Result<AdvertisingSlot> saveSlot(@RequestBody AdvertisingSlot slot) {
        return Result.success(advertisingService.saveSlot(slot));
    }

    @PostMapping("/slots/{slotId}/status")
    @RequirePermission("advertising:slot:manage")
    public Result<Void> updateSlotStatus(
            @PathVariable Long slotId,
            @RequestParam String status) {
        advertisingService.updateSlotStatus(slotId, status);
        return Result.success(null);
    }

    @GetMapping("/slots/{slotId}/stats")
    @RequirePermission("advertising:slot:manage")
    public Result<Map<String, Object>> getSlotStats(@PathVariable Long slotId) {
        return Result.success(advertisingService.adminGetSlotStats(slotId));
    }

    // ==================== 审核管理 ====================

    @GetMapping("/campaigns")
    @RequirePermission("advertising:campaign:review")
    public Result<PageResult<AdvertisingCampaign>> listCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(advertisingService.adminListCampaigns(status, companyId, pageNum, pageSize));
    }

    @PostMapping("/campaigns/{campaignId}/approve")
    @RequirePermission("advertising:campaign:review")
    public Result<Void> approve(
            @PathVariable Long campaignId,
            @RequestParam(required = false) String note) {
        Long reviewerId = SecurityUtils.requireUserId();
        advertisingService.approve(campaignId, reviewerId, note);
        return Result.success(null);
    }

    @PostMapping("/campaigns/{campaignId}/reject")
    @RequirePermission("advertising:campaign:review")
    public Result<Void> reject(
            @PathVariable Long campaignId,
            @RequestParam String reason) {
        Long reviewerId = SecurityUtils.requireUserId();
        advertisingService.reject(campaignId, reviewerId, reason);
        return Result.success(null);
    }

    // ==================== 商机推荐管理 ====================

    @PostMapping("/lead-recommendations/batch-generate")
    @RequirePermission("advertising:campaign:review")
    public Result<Integer> batchGenerate(
            @RequestParam(defaultValue = "5") int perCompanyLimit) {
        return Result.success(leadRecommendationService.batchGenerateForAllCompanies(perCompanyLimit));
    }

    @GetMapping("/lead-recommendations")
    @RequirePermission("advertising:campaign:review")
    public Result<?> adminListRecommendations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(leadRecommendationService.adminListRecommendations(status, companyId, pageNum, pageSize));
    }
}