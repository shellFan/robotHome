package com.robot.home.subscription.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.PageUtils;
import com.robot.home.security.RequirePermission;
import com.robot.home.subscription.entity.UserSubscription;
import com.robot.home.subscription.mapper.UserSubscriptionMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 订阅管理接口（需要权限）
 */
@RestController
@RequestMapping("/api/admin/subscriptions")
public class AdminSubscriptionController {

    @Resource
    private UserSubscriptionMapper subscriptionMapper;

    /**
     * 管理端查看订阅列表
     */
    @GetMapping
    @RequirePermission("subscription:list")
    public Result<PageResult<UserSubscription>> list(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<UserSubscription> page = new Page<>(pn, ps);
        IPage<UserSubscription> result = subscriptionMapper.selectPage(page,
                Wrappers.<UserSubscription>lambdaQuery()
                        .eq(StrUtil.isNotBlank(targetType), UserSubscription::getTargetType, targetType)
                        .eq(targetId != null, UserSubscription::getTargetId, targetId)
                        .eq(userId != null, UserSubscription::getUserId, userId)
                        .orderByDesc(UserSubscription::getCreateTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }
}