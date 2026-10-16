package com.robot.home.advertising.service;

import com.robot.home.common.PageResult;
import com.robot.home.advertising.entity.AdvertisingCampaign;
import com.robot.home.advertising.entity.AdvertisingCreative;
import com.robot.home.advertising.entity.AdvertisingSlot;

import java.util.List;
import java.util.Map;

/**
 * 广告服务
 * <p>
 * 审核状态机：DRAFT→PENDING_REVIEW→APPROVED/REJECTED→ONLINE→OFFLINE→ENDED
 * 事件去重：uk_event_dedup唯一索引防刷量
 */
public interface AdvertisingService {

    // ==================== 广告位管理（Admin） ====================

    List<AdvertisingSlot> listSlots();
    AdvertisingSlot getSlot(Long slotId);
    AdvertisingSlot saveSlot(AdvertisingSlot slot);
    void updateSlotStatus(Long slotId, String status);

    // ==================== 投放计划管理（企业） ====================

    /**
     * 创建投放计划（DRAFT状态）
     */
    AdvertisingCampaign createCampaign(Long companyId, Long slotId, String name,
                                       java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
                                       Long budgetCents, Integer dailyLimit, Integer totalLimit);

    /**
     * 提交审核：DRAFT/REJECTED→PENDING_REVIEW
     */
    void submitForReview(Long campaignId, Long companyId);

    /**
     * Admin审核通过：PENDING_REVIEW→APPROVED
     */
    void approve(Long campaignId, Long reviewerId, String note);

    /**
     * Admin审核拒绝：PENDING_REVIEW→REJECTED
     */
    void reject(Long campaignId, Long reviewerId, String reason);

    /**
     * 上线：APPROVED→ONLINE
     */
    void goOnline(Long campaignId, Long companyId);

    /**
     * 下线：ONLINE→OFFLINE
     */
    void goOffline(Long campaignId, Long companyId);

    /**
     * 获取企业投放计划列表
     */
    PageResult<AdvertisingCampaign> listCompanyCampaigns(Long companyId, String status, Integer pageNum, Integer pageSize);

    /**
     * Admin: 分页查询所有投放计划
     */
    PageResult<AdvertisingCampaign> adminListCampaigns(String status, Long companyId, Integer pageNum, Integer pageSize);

    AdvertisingCampaign getCampaign(Long campaignId);

    // ==================== 素材管理 ====================

    AdvertisingCreative createCreative(Long campaignId, Long companyId, String mediaType,
                                       String mediaUrl, String jumpUrl, String title, String description);
    List<AdvertisingCreative> listCreatives(Long campaignId);

    // ==================== 展示与事件 ====================

    /**
     * 获取广告位展示内容（仅ONLINE+时间范围内）
     */
    List<Map<String, Object>> getSlotAds(String slotCode);

    /**
     * 记录广告事件（IMPRESSION/CLICK），带去重
     */
    void recordEvent(Long campaignId, Long slotId, String eventType, String visitorId, String ip);

    /**
     * 获取投放计划统计
     */
    Map<String, Object> getCampaignStats(Long campaignId);

    /**
     * Admin: 获取广告位统计
     */
    Map<String, Object> adminGetSlotStats(Long slotId);
}