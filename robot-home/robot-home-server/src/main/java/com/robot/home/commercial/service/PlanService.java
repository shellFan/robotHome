package com.robot.home.commercial.service;

import com.robot.home.common.PageResult;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterprisePlanVersion;

import java.util.List;

/**
 * 套餐管理服务
 */
public interface PlanService {

    /**
     * 公开套餐列表（仅上架+可售）
     */
    List<EnterprisePlan> listOnShelfPlans();

    /**
     * 套餐详情（含当前版本权益配置）
     */
    EnterprisePlan getPlanDetail(Long planId);

    /**
     * 获取套餐当前活跃版本
     */
    EnterprisePlanVersion getActiveVersion(Long planId);

    /**
     * Admin: 套餐列表（全部）
     */
    PageResult<EnterprisePlan> adminListPlans(String status, Integer pageNum, Integer pageSize);

    /**
     * Admin: 创建/更新套餐
     */
    EnterprisePlan savePlan(EnterprisePlan plan);

    /**
     * Admin: 上架/下架
     */
    void updatePlanStatus(Long planId, String status);

    /**
     * Admin: 创建新版本（价格+权益配置快照）
     */
    EnterprisePlanVersion createPlanVersion(Long planId, Long priceCents, Integer durationDays, String entitlementConfig);

    /**
     * Admin: 版本列表
     */
    List<EnterprisePlanVersion> listVersions(Long planId);
}