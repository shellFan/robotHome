package com.robot.home.advertising.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.advertising.entity.AdvertisingCampaign;
import com.robot.home.advertising.entity.AdvertisingCreative;
import com.robot.home.advertising.service.AdvertisingService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 广告接口（企业端+公开展示）
 */
@RestController
@RequestMapping("/api/advertising")
public class AdvertisingController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private AdvertisingService advertisingService;
    @Resource
    private CompanyMemberService companyMemberService;

    // ==================== 公开接口 ====================

    /**
     * 获取广告位展示内容（公开，无需登录）
     */
    @GetMapping("/slots/{slotCode}/ads")
    public Result<List<Map<String, Object>>> getSlotAds(@PathVariable String slotCode) {
        return Result.success(advertisingService.getSlotAds(slotCode));
    }

    /**
     * 记录广告事件（公开，无需登录）
     */
    @PostMapping("/events")
    public Result<Void> recordEvent(
            @RequestParam Long campaignId,
            @RequestParam Long slotId,
            @RequestParam String eventType,
            @RequestParam(required = false) String visitorId,
            HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        if (visitorId == null || visitorId.isEmpty()) {
            visitorId = ip != null ? ip : "anonymous";
        }
        advertisingService.recordEvent(campaignId, slotId, eventType, visitorId, ip);
        return Result.success(null);
    }

    // ==================== 企业端接口 ====================

    /**
     * 广告位列表（企业可查看可用广告位）
     */
    @GetMapping("/slots")
    public Result<?> listSlots() {
        return Result.success(advertisingService.listSlots());
    }

    /**
     * 创建投放计划
     */
    @PostMapping("/campaigns")
    public Result<AdvertisingCampaign> createCampaign(
            @RequestParam Long companyId,
            @RequestParam Long slotId,
            @RequestParam String name,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Long budgetCents,
            @RequestParam(required = false) Integer dailyLimit,
            @RequestParam(required = false) Integer totalLimit) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(advertisingService.createCampaign(
                companyId, slotId, name,
                LocalDateTime.parse(startTime, DT_FMT),
                LocalDateTime.parse(endTime, DT_FMT),
                budgetCents, dailyLimit, totalLimit));
    }

    /**
     * 我的投放计划列表
     */
    @GetMapping("/campaigns")
    public Result<PageResult<AdvertisingCampaign>> listCampaigns(
            @RequestParam Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(advertisingService.listCompanyCampaigns(companyId, status, pageNum, pageSize));
    }

    /**
     * 提交审核
     */
    @PostMapping("/campaigns/{campaignId}/submit")
    public Result<Void> submitForReview(
            @PathVariable Long campaignId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        advertisingService.submitForReview(campaignId, companyId);
        return Result.success(null);
    }

    /**
     * 上线
     */
    @PostMapping("/campaigns/{campaignId}/online")
    public Result<Void> goOnline(
            @PathVariable Long campaignId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        advertisingService.goOnline(campaignId, companyId);
        return Result.success(null);
    }

    /**
     * 下线
     */
    @PostMapping("/campaigns/{campaignId}/offline")
    public Result<Void> goOffline(
            @PathVariable Long campaignId,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        advertisingService.goOffline(campaignId, companyId);
        return Result.success(null);
    }

    /**
     * 添加素材
     */
    @PostMapping("/campaigns/{campaignId}/creatives")
    public Result<AdvertisingCreative> createCreative(
            @PathVariable Long campaignId,
            @RequestParam Long companyId,
            @RequestParam String mediaType,
            @RequestParam String mediaUrl,
            @RequestParam(required = false) String jumpUrl,
            @RequestParam String title,
            @RequestParam(required = false) String description) {
        Long userId = SecurityUtils.requireUserId();
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(advertisingService.createCreative(
                campaignId, companyId, mediaType, mediaUrl, jumpUrl, title, description));
    }

    /**
     * 素材列表
     */
    @GetMapping("/campaigns/{campaignId}/creatives")
    public Result<List<AdvertisingCreative>> listCreatives(@PathVariable Long campaignId) {
        return Result.success(advertisingService.listCreatives(campaignId));
    }

    /**
     * 投放统计
     */
    @GetMapping("/campaigns/{campaignId}/stats")
    public Result<Map<String, Object>> getCampaignStats(@PathVariable Long campaignId) {
        return Result.success(advertisingService.getCampaignStats(campaignId));
    }
}