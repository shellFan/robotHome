package com.robot.home.advertising.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.advertising.entity.AdvertisingCampaign;
import com.robot.home.advertising.entity.AdvertisingCreative;
import com.robot.home.advertising.entity.AdvertisingEvent;
import com.robot.home.advertising.entity.AdvertisingSlot;
import com.robot.home.advertising.mapper.AdvertisingCampaignMapper;
import com.robot.home.advertising.mapper.AdvertisingCreativeMapper;
import com.robot.home.advertising.mapper.AdvertisingEventMapper;
import com.robot.home.advertising.mapper.AdvertisingSlotMapper;
import com.robot.home.advertising.service.AdvertisingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 广告服务实现
 * <p>
 * 核心安全设计：
 * 1. 审核状态机CAS：每次状态变更WHERE status=EXPECTED
 * 2. 展示仅返回ONLINE+时间范围内+有素材的计划
 * 3. 事件去重：dedupKey = campaignId:eventType:visitorId:date
 * 4. 企业隔离：只能操作自己的投放计划
 */
@Slf4j
@Service
public class AdvertisingServiceImpl implements AdvertisingService {

    @Resource
    private AdvertisingSlotMapper slotMapper;
    @Resource
    private AdvertisingCampaignMapper campaignMapper;
    @Resource
    private AdvertisingCreativeMapper creativeMapper;
    @Resource
    private AdvertisingEventMapper eventMapper;

    // ==================== 广告位管理 ====================

    @Override
    public List<AdvertisingSlot> listSlots() {
        return slotMapper.selectList(
                Wrappers.<AdvertisingSlot>lambdaQuery()
                        .eq(AdvertisingSlot::getStatus, "ACTIVE")
                        .orderByAsc(AdvertisingSlot::getSort));
    }

    @Override
    public AdvertisingSlot getSlot(Long slotId) {
        AdvertisingSlot slot = slotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException("广告位不存在");
        }
        return slot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdvertisingSlot saveSlot(AdvertisingSlot slot) {
        if (StrUtil.isBlank(slot.getSlotCode()) || StrUtil.isBlank(slot.getName())) {
            throw new BusinessException("广告位编码和名称不能为空");
        }
        if (slot.getId() != null) {
            slotMapper.updateById(slot);
        } else {
            if (slot.getStatus() == null) slot.setStatus("ACTIVE");
            if (slot.getSort() == null) slot.setSort(0);
            slotMapper.insert(slot);
        }
        return slot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSlotStatus(Long slotId, String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new BusinessException("无效状态: " + status);
        }
        int updated = slotMapper.update(null,
                Wrappers.<AdvertisingSlot>lambdaUpdate()
                        .set(AdvertisingSlot::getStatus, status)
                        .eq(AdvertisingSlot::getId, slotId));
        if (updated == 0) {
            throw new BusinessException("广告位不存在");
        }
    }

    // ==================== 投放计划管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdvertisingCampaign createCampaign(Long companyId, Long slotId, String name,
                                               LocalDateTime startTime, LocalDateTime endTime,
                                               Long budgetCents, Integer dailyLimit, Integer totalLimit) {
        if (companyId == null || slotId == null || StrUtil.isBlank(name)) {
            throw new BusinessException("参数不完整");
        }
        AdvertisingSlot slot = getSlot(slotId);
        if (!"ACTIVE".equals(slot.getStatus())) {
            throw new BusinessException("广告位已停用");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException("投放时间无效");
        }
        AdvertisingCampaign campaign = new AdvertisingCampaign();
        campaign.setCompanyId(companyId);
        campaign.setSlotId(slotId);
        campaign.setName(name);
        campaign.setStatus("DRAFT");
        campaign.setStartTime(startTime);
        campaign.setEndTime(endTime);
        campaign.setBudgetCents(budgetCents != null ? budgetCents : 0L);
        campaign.setDailyLimit(dailyLimit != null ? dailyLimit : 0);
        campaign.setTotalLimit(totalLimit != null ? totalLimit : 0);
        campaignMapper.insert(campaign);
        log.info("创建投放计划: campaignId={}, companyId={}, slotId={}", campaign.getId(), companyId, slotId);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForReview(Long campaignId, Long companyId) {
        AdvertisingCampaign campaign = getCampaignAndCheckOwner(campaignId, companyId);
        // 必须有素材才能提交审核
        long creativeCount = creativeMapper.selectCount(
                Wrappers.<AdvertisingCreative>lambdaQuery()
                        .eq(AdvertisingCreative::getCampaignId, campaignId));
        if (creativeCount == 0) {
            throw new BusinessException("请先上传广告素材再提交审核");
        }
        // CAS: DRAFT/REJECTED→PENDING_REVIEW
        int updated = campaignMapper.update(null,
                Wrappers.<AdvertisingCampaign>lambdaUpdate()
                        .set(AdvertisingCampaign::getStatus, "PENDING_REVIEW")
                        .eq(AdvertisingCampaign::getId, campaignId)
                        .in(AdvertisingCampaign::getStatus, "DRAFT", "REJECTED"));
        if (updated == 0) {
            throw new BusinessException("当前状态不支持提交审核");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long campaignId, Long reviewerId, String note) {
        int updated = campaignMapper.update(null,
                Wrappers.<AdvertisingCampaign>lambdaUpdate()
                        .set(AdvertisingCampaign::getStatus, "APPROVED")
                        .set(AdvertisingCampaign::getReviewerId, reviewerId)
                        .set(AdvertisingCampaign::getReviewTime, LocalDateTime.now())
                        .set(AdvertisingCampaign::getReviewNote, note)
                        .eq(AdvertisingCampaign::getId, campaignId)
                        .eq(AdvertisingCampaign::getStatus, "PENDING_REVIEW"));
        if (updated == 0) {
            throw new BusinessException("当前状态不支持审核通过");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long campaignId, Long reviewerId, String reason) {
        int updated = campaignMapper.update(null,
                Wrappers.<AdvertisingCampaign>lambdaUpdate()
                        .set(AdvertisingCampaign::getStatus, "REJECTED")
                        .set(AdvertisingCampaign::getReviewerId, reviewerId)
                        .set(AdvertisingCampaign::getReviewTime, LocalDateTime.now())
                        .set(AdvertisingCampaign::getReviewNote, reason)
                        .eq(AdvertisingCampaign::getId, campaignId)
                        .eq(AdvertisingCampaign::getStatus, "PENDING_REVIEW"));
        if (updated == 0) {
            throw new BusinessException("当前状态不支持审核拒绝");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void goOnline(Long campaignId, Long companyId) {
        AdvertisingCampaign campaign = getCampaignAndCheckOwner(campaignId, companyId);
        if (!"APPROVED".equals(campaign.getStatus())) {
            throw new BusinessException("仅审核通过的计划可上线");
        }
        LocalDateTime now = LocalDateTime.now();
        if (campaign.getEndTime().isBefore(now)) {
            throw new BusinessException("投放时间已结束");
        }
        int updated = campaignMapper.update(null,
                Wrappers.<AdvertisingCampaign>lambdaUpdate()
                        .set(AdvertisingCampaign::getStatus, "ONLINE")
                        .eq(AdvertisingCampaign::getId, campaignId)
                        .eq(AdvertisingCampaign::getStatus, "APPROVED"));
        if (updated == 0) {
            throw new BusinessException("上线失败，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void goOffline(Long campaignId, Long companyId) {
        getCampaignAndCheckOwner(campaignId, companyId);
        int updated = campaignMapper.update(null,
                Wrappers.<AdvertisingCampaign>lambdaUpdate()
                        .set(AdvertisingCampaign::getStatus, "OFFLINE")
                        .eq(AdvertisingCampaign::getId, campaignId)
                        .in(AdvertisingCampaign::getStatus, "ONLINE"));
        if (updated == 0) {
            throw new BusinessException("当前状态不支持下线");
        }
    }

    @Override
    public PageResult<AdvertisingCampaign> listCompanyCampaigns(Long companyId, String status, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<AdvertisingCampaign> page = new Page<>(pn, ps);
        Page<AdvertisingCampaign> result = campaignMapper.selectPage(page,
                Wrappers.<AdvertisingCampaign>lambdaQuery()
                        .eq(AdvertisingCampaign::getCompanyId, companyId)
                        .eq(StrUtil.isNotBlank(status), AdvertisingCampaign::getStatus, status)
                        .orderByDesc(AdvertisingCampaign::getId));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<AdvertisingCampaign> adminListCampaigns(String status, Long companyId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<AdvertisingCampaign> page = new Page<>(pn, ps);
        Page<AdvertisingCampaign> result = campaignMapper.selectPage(page,
                Wrappers.<AdvertisingCampaign>lambdaQuery()
                        .eq(StrUtil.isNotBlank(status), AdvertisingCampaign::getStatus, status)
                        .eq(companyId != null, AdvertisingCampaign::getCompanyId, companyId)
                        .orderByDesc(AdvertisingCampaign::getId));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public AdvertisingCampaign getCampaign(Long campaignId) {
        AdvertisingCampaign campaign = campaignMapper.selectById(campaignId);
        if (campaign == null) {
            throw new BusinessException("投放计划不存在");
        }
        return campaign;
    }

    // ==================== 素材管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdvertisingCreative createCreative(Long campaignId, Long companyId, String mediaType,
                                               String mediaUrl, String jumpUrl, String title, String description) {
        AdvertisingCampaign campaign = getCampaignAndCheckOwner(campaignId, companyId);
        // 仅DRAFT/REJECTED状态可添加素材
        if (!"DRAFT".equals(campaign.getStatus()) && !"REJECTED".equals(campaign.getStatus())) {
            throw new BusinessException("仅草稿/被拒状态的计划可添加素材");
        }
        if (!"IMAGE".equals(mediaType) && !"VIDEO".equals(mediaType)) {
            throw new BusinessException("素材类型仅支持IMAGE/VIDEO");
        }
        if (StrUtil.isBlank(mediaUrl) || StrUtil.isBlank(title)) {
            throw new BusinessException("素材URL和标题不能为空");
        }
        // 安全：jumpUrl仅允许http/https协议，禁止javascript:/data:/vbscript:等危险协议
        if (StrUtil.isNotBlank(jumpUrl)) {
            String normalized = jumpUrl.trim().toLowerCase();
            if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
                throw new BusinessException("跳转链接仅支持http/https协议");
            }
        }
        AdvertisingCreative creative = new AdvertisingCreative();
        creative.setCampaignId(campaignId);
        creative.setMediaType(mediaType);
        creative.setMediaUrl(mediaUrl);
        creative.setJumpUrl(jumpUrl);
        creative.setTitle(title);
        creative.setDescription(description);
        creativeMapper.insert(creative);
        return creative;
    }

    @Override
    public List<AdvertisingCreative> listCreatives(Long campaignId) {
        return creativeMapper.selectList(
                Wrappers.<AdvertisingCreative>lambdaQuery()
                        .eq(AdvertisingCreative::getCampaignId, campaignId));
    }

    // ==================== 展示与事件 ====================

    @Override
    public List<Map<String, Object>> getSlotAds(String slotCode) {
        if (StrUtil.isBlank(slotCode)) {
            return new ArrayList<>();
        }
        AdvertisingSlot slot = slotMapper.selectOne(
                Wrappers.<AdvertisingSlot>lambdaQuery()
                        .eq(AdvertisingSlot::getSlotCode, slotCode)
                        .eq(AdvertisingSlot::getStatus, "ACTIVE"));
        if (slot == null) {
            return new ArrayList<>();
        }
        LocalDateTime now = LocalDateTime.now();
        // 查ONLINE+时间范围内的计划
        List<AdvertisingCampaign> campaigns = campaignMapper.selectList(
                Wrappers.<AdvertisingCampaign>lambdaQuery()
                        .eq(AdvertisingCampaign::getSlotId, slot.getId())
                        .eq(AdvertisingCampaign::getStatus, "ONLINE")
                        .le(AdvertisingCampaign::getStartTime, now)
                        .ge(AdvertisingCampaign::getEndTime, now));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Map<String, Object>> ads = new ArrayList<>();
        for (AdvertisingCampaign campaign : campaigns) {
            // 投放限额检查：totalLimit>0时检查总展示数，dailyLimit>0时检查当日展示数
            if (campaign.getTotalLimit() != null && campaign.getTotalLimit() > 0) {
                long totalImpressions = eventMapper.selectCount(
                        Wrappers.<AdvertisingEvent>lambdaQuery()
                                .eq(AdvertisingEvent::getCampaignId, campaign.getId())
                                .eq(AdvertisingEvent::getEventType, "IMPRESSION"));
                if (totalImpressions >= campaign.getTotalLimit()) {
                    continue; // 已达总展示上限，不再展示
                }
            }
            if (campaign.getDailyLimit() != null && campaign.getDailyLimit() > 0) {
                long todayImpressions = eventMapper.selectCount(
                        Wrappers.<AdvertisingEvent>lambdaQuery()
                                .eq(AdvertisingEvent::getCampaignId, campaign.getId())
                                .eq(AdvertisingEvent::getEventType, "IMPRESSION")
                                .ge(AdvertisingEvent::getEventTime, todayStart));
                if (todayImpressions >= campaign.getDailyLimit()) {
                    continue; // 已达当日展示上限，不再展示
                }
            }
            // 查第一个素材
            AdvertisingCreative creative = creativeMapper.selectOne(
                    Wrappers.<AdvertisingCreative>lambdaQuery()
                            .eq(AdvertisingCreative::getCampaignId, campaign.getId())
                            .last("LIMIT 1"));
            if (creative != null) {
                Map<String, Object> ad = new HashMap<>();
                ad.put("campaignId", campaign.getId());
                ad.put("slotId", slot.getId());
                ad.put("title", creative.getTitle());
                ad.put("description", creative.getDescription());
                ad.put("mediaType", creative.getMediaType());
                ad.put("mediaUrl", creative.getMediaUrl());
                ad.put("jumpUrl", creative.getJumpUrl());
                ads.add(ad);
            }
        }
        return ads;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordEvent(Long campaignId, Long slotId, String eventType, String visitorId, String ip) {
        if (!"IMPRESSION".equals(eventType) && !"CLICK".equals(eventType)) {
            return;
        }
        AdvertisingEvent event = new AdvertisingEvent();
        event.setCampaignId(campaignId);
        event.setSlotId(slotId);
        event.setEventType(eventType);
        event.setVisitorId(visitorId);
        event.setIp(ip);
        // 去重键：campaignId:eventType:visitorId:date
        String dedupKey = campaignId + ":" + eventType + ":" + visitorId + ":" + LocalDate.now();
        event.setDedupKey(dedupKey);
        event.setEventTime(LocalDateTime.now());
        try {
            eventMapper.insert(event);
        } catch (DuplicateKeyException e) {
            // 重复事件（同一访客同一天同一类型），静默跳过
            log.debug("广告事件去重跳过: {}", dedupKey);
        }
    }

    @Override
    public Map<String, Object> getCampaignStats(Long campaignId) {
        Map<String, Object> stats = new HashMap<>();
        long impressions = eventMapper.selectCount(
                Wrappers.<AdvertisingEvent>lambdaQuery()
                        .eq(AdvertisingEvent::getCampaignId, campaignId)
                        .eq(AdvertisingEvent::getEventType, "IMPRESSION"));
        long clicks = eventMapper.selectCount(
                Wrappers.<AdvertisingEvent>lambdaQuery()
                        .eq(AdvertisingEvent::getCampaignId, campaignId)
                        .eq(AdvertisingEvent::getEventType, "CLICK"));
        stats.put("impressions", impressions);
        stats.put("clicks", clicks);
        stats.put("ctr", impressions > 0 ? (double) clicks / impressions : 0.0);
        return stats;
    }

    @Override
    public Map<String, Object> adminGetSlotStats(Long slotId) {
        Map<String, Object> stats = new HashMap<>();
        long totalCampaigns = campaignMapper.selectCount(
                Wrappers.<AdvertisingCampaign>lambdaQuery()
                        .eq(AdvertisingCampaign::getSlotId, slotId));
        long onlineCampaigns = campaignMapper.selectCount(
                Wrappers.<AdvertisingCampaign>lambdaQuery()
                        .eq(AdvertisingCampaign::getSlotId, slotId)
                        .eq(AdvertisingCampaign::getStatus, "ONLINE"));
        long pendingReview = campaignMapper.selectCount(
                Wrappers.<AdvertisingCampaign>lambdaQuery()
                        .eq(AdvertisingCampaign::getSlotId, slotId)
                        .eq(AdvertisingCampaign::getStatus, "PENDING_REVIEW"));
        long impressions = eventMapper.selectCount(
                Wrappers.<AdvertisingEvent>lambdaQuery()
                        .eq(AdvertisingEvent::getSlotId, slotId)
                        .eq(AdvertisingEvent::getEventType, "IMPRESSION"));
        long clicks = eventMapper.selectCount(
                Wrappers.<AdvertisingEvent>lambdaQuery()
                        .eq(AdvertisingEvent::getSlotId, slotId)
                        .eq(AdvertisingEvent::getEventType, "CLICK"));
        stats.put("totalCampaigns", totalCampaigns);
        stats.put("onlineCampaigns", onlineCampaigns);
        stats.put("pendingReview", pendingReview);
        stats.put("impressions", impressions);
        stats.put("clicks", clicks);
        return stats;
    }

    // ==================== 私有方法 ====================

    private AdvertisingCampaign getCampaignAndCheckOwner(Long campaignId, Long companyId) {
        AdvertisingCampaign campaign = campaignMapper.selectById(campaignId);
        if (campaign == null) {
            throw new BusinessException("投放计划不存在");
        }
        if (!campaign.getCompanyId().equals(companyId)) {
            throw new BusinessException("无权操作此投放计划");
        }
        return campaign;
    }
}