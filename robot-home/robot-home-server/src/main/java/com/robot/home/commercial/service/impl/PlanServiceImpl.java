package com.robot.home.commercial.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterprisePlanVersion;
import com.robot.home.commercial.mapper.EnterprisePlanMapper;
import com.robot.home.commercial.mapper.EnterprisePlanVersionMapper;
import com.robot.home.commercial.service.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 套餐管理服务实现
 */
@Slf4j
@Service
public class PlanServiceImpl implements PlanService {

    private static final List<String> VALID_PLAN_STATUS =
            java.util.Arrays.asList("DRAFT", "ON_SHELF", "OFF_SHELF");

    @Resource
    private EnterprisePlanMapper planMapper;
    @Resource
    private EnterprisePlanVersionMapper planVersionMapper;

    @Override
    public List<EnterprisePlan> listOnShelfPlans() {
        return planMapper.selectList(Wrappers.<EnterprisePlan>lambdaQuery()
                .eq(EnterprisePlan::getStatus, "ON_SHELF")
                .eq(EnterprisePlan::getSaleable, 1)
                .orderByAsc(EnterprisePlan::getSort));
    }

    @Override
    public EnterprisePlan getPlanDetail(Long planId) {
        EnterprisePlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("套餐不存在");
        }
        return plan;
    }

    @Override
    public EnterprisePlanVersion getActiveVersion(Long planId) {
        EnterprisePlan plan = getPlanDetail(planId);
        return planVersionMapper.selectOne(Wrappers.<EnterprisePlanVersion>lambdaQuery()
                .eq(EnterprisePlanVersion::getPlanId, planId)
                .eq(EnterprisePlanVersion::getVersion, plan.getCurrentVersion())
                .eq(EnterprisePlanVersion::getStatus, "ACTIVE"));
    }

    @Override
    public PageResult<EnterprisePlan> adminListPlans(String status, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<EnterprisePlan> page = new Page<>(pn, ps);
        IPage<EnterprisePlan> result = planMapper.selectPage(page,
                Wrappers.<EnterprisePlan>lambdaQuery()
                        .eq(StrUtil.isNotBlank(status), EnterprisePlan::getStatus, status)
                        .orderByAsc(EnterprisePlan::getSort));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EnterprisePlan savePlan(EnterprisePlan plan) {
        if (StrUtil.isBlank(plan.getCode()) || StrUtil.isBlank(plan.getName())) {
            throw new BusinessException("套餐编码和名称不能为空");
        }
        if (plan.getStatus() != null && !VALID_PLAN_STATUS.contains(plan.getStatus())) {
            throw new BusinessException("无效的套餐状态");
        }
        if (plan.getId() != null) {
            EnterprisePlan existing = planMapper.selectById(plan.getId());
            if (existing == null) {
                throw new BusinessException("套餐不存在");
            }
            // 不允许修改code（避免破坏已引用的套餐）
            plan.setCode(existing.getCode());
            planMapper.updateById(plan);
            return planMapper.selectById(plan.getId());
        }
        // 新建：默认DRAFT
        if (plan.getStatus() == null) {
            plan.setStatus("DRAFT");
        }
        if (plan.getCurrentVersion() == null) {
            plan.setCurrentVersion(0);
        }
        if (plan.getSaleable() == null) {
            plan.setSaleable(0);
        }
        planMapper.insert(plan);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlanStatus(Long planId, String status) {
        if (!VALID_PLAN_STATUS.contains(status)) {
            throw new BusinessException("无效的套餐状态");
        }
        EnterprisePlan plan = getPlanDetail(planId);
        // 上架前提：有活跃版本
        if ("ON_SHELF".equals(status) && getActiveVersion(planId) == null) {
            throw new BusinessException("套餐没有活跃版本，不能上架");
        }
        planMapper.update(null, Wrappers.<EnterprisePlan>lambdaUpdate()
                .set(EnterprisePlan::getStatus, status)
                .set(EnterprisePlan::getSaleable, "ON_SHELF".equals(status) ? 1 : 0)
                .eq(EnterprisePlan::getId, planId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EnterprisePlanVersion createPlanVersion(Long planId, Long priceCents, Integer durationDays, String entitlementConfig) {
        EnterprisePlan plan = getPlanDetail(planId);
        if (priceCents == null || priceCents < 0) {
            throw new BusinessException("价格不能为负");
        }
        if (durationDays == null || durationDays <= 0) {
            throw new BusinessException("有效期必须大于0");
        }
        // 新版本号 = 当前版本+1
        int newVersion = (plan.getCurrentVersion() == null ? 0 : plan.getCurrentVersion()) + 1;
        EnterprisePlanVersion version = new EnterprisePlanVersion();
        version.setPlanId(planId);
        version.setVersion(newVersion);
        version.setName(plan.getName() + "V" + newVersion);
        version.setPriceCents(priceCents);
        version.setDurationDays(durationDays);
        version.setEntitlementConfig(entitlementConfig);
        version.setStatus("ACTIVE");
        version.setEffectiveTime(LocalDateTime.now());
        planVersionMapper.insert(version);
        // 更新套餐当前版本
        planMapper.update(null, Wrappers.<EnterprisePlan>lambdaUpdate()
                .set(EnterprisePlan::getCurrentVersion, newVersion)
                .eq(EnterprisePlan::getId, planId));
        // 归档旧版本
        planVersionMapper.update(null, Wrappers.<EnterprisePlanVersion>lambdaUpdate()
                .set(EnterprisePlanVersion::getStatus, "ARCHIVED")
                .eq(EnterprisePlanVersion::getPlanId, planId)
                .ne(EnterprisePlanVersion::getVersion, newVersion)
                .eq(EnterprisePlanVersion::getStatus, "ACTIVE"));
        return version;
    }

    @Override
    public List<EnterprisePlanVersion> listVersions(Long planId) {
        return planVersionMapper.selectList(Wrappers.<EnterprisePlanVersion>lambdaQuery()
                .eq(EnterprisePlanVersion::getPlanId, planId)
                .orderByDesc(EnterprisePlanVersion::getVersion));
    }
}