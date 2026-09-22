package com.robot.home.subscription.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.subscription.entity.UserSubscription;
import com.robot.home.subscription.mapper.UserSubscriptionMapper;
import com.robot.home.subscription.service.SubscriptionService;
import com.robot.home.subscription.vo.SubscriptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订阅服务实现
 */
@Slf4j
@Service
public class SubscriptionServiceImpl extends ServiceImpl<UserSubscriptionMapper, UserSubscription>
        implements SubscriptionService {

    /** 合法的订阅目标类型 */
    private static final Set<String> VALID_TARGET_TYPES = new HashSet<>(Arrays.asList(
            Constants.SUB_TARGET_ROBOT, Constants.SUB_TARGET_BRAND, Constants.SUB_TARGET_COMPANY));

    /** 合法的订阅事件类型 */
    private static final Set<String> VALID_EVENT_TYPES = new HashSet<>(Arrays.asList(
            Constants.SUB_EVENT_NEW_CONTENT, Constants.SUB_EVENT_PARAM_CHANGE,
            Constants.SUB_EVENT_PRICE_CHANGE, Constants.SUB_EVENT_NEW_REVIEW,
            Constants.SUB_EVENT_NEW_QA, Constants.SUB_EVENT_PRODUCT_RELEASE,
            Constants.SUB_EVENT_IMPORTANT_CHANGE));

    /** 订阅状态缓存TTL（秒） */
    private static final long CACHE_TTL_SECONDS = 300L;

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public boolean checkSubscribed(Long userId, String targetType, Long targetId) {
        if (userId == null) {
            return false;
        }
        validateTargetType(targetType);
        // 先查缓存
        String cacheKey = buildCacheKey(userId, targetType, targetId);
        try {
            String cached = redisUtils.get(cacheKey);
            if (cached != null) {
                return "1".equals(cached);
            }
        } catch (Exception e) {
            log.warn("Redis读取订阅状态异常, 降级DB: {}", e.getMessage());
        }
        // 查DB
        boolean subscribed = count(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getUserId, userId)
                .eq(UserSubscription::getTargetType, targetType)
                .eq(UserSubscription::getTargetId, targetId)
                .eq(UserSubscription::getEnabled, 1)) > 0;
        // 回写缓存
        try {
            redisUtils.set(cacheKey, subscribed ? "1" : "0", CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis写入订阅状态异常: {}", e.getMessage());
        }
        return subscribed;
    }

    @Override
    public Set<Long> checkBatchSubscribed(Long userId, String targetType, List<Long> targetIds) {
        if (userId == null || targetIds == null || targetIds.isEmpty()) {
            return Collections.emptySet();
        }
        validateTargetType(targetType);
        return list(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getUserId, userId)
                .eq(UserSubscription::getTargetType, targetType)
                .in(UserSubscription::getTargetId, targetIds)
                .eq(UserSubscription::getEnabled, 1))
                .stream().map(UserSubscription::getTargetId).collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, String targetType, Long targetId) {
        validateTargetType(targetType);
        // 先尝试删除（取消订阅）
        int deleted = baseMapper.delete(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getUserId, userId)
                .eq(UserSubscription::getTargetType, targetType)
                .eq(UserSubscription::getTargetId, targetId));
        if (deleted > 0) {
            // 删除缓存
            evictCache(userId, targetType, targetId);
            return false;
        }
        // 不存在则新增（订阅），利用唯一索引防重复
        UserSubscription sub = new UserSubscription();
        sub.setUserId(userId);
        sub.setTargetType(targetType);
        sub.setTargetId(targetId);
        // 默认订阅全部事件类型
        sub.setEventTypes(String.join(",", VALID_EVENT_TYPES));
        sub.setEnabled(1);
        try {
            save(sub);
        } catch (DuplicateKeyException e) {
            // 并发订阅，幂等返回
            return true;
        }
        evictCache(userId, targetType, targetId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEventTypes(Long userId, String targetType, Long targetId, String eventTypes) {
        validateTargetType(targetType);
        validateEventTypes(eventTypes);
        UserSubscription existing = getOne(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getUserId, userId)
                .eq(UserSubscription::getTargetType, targetType)
                .eq(UserSubscription::getTargetId, targetId));
        if (existing == null) {
            throw new BusinessException("订阅记录不存在");
        }
        existing.setEventTypes(eventTypes);
        updateById(existing);
        evictCache(userId, targetType, targetId);
    }

    @Override
    public PageResult<SubscriptionVO> mySubscriptions(Long userId, String targetType, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<UserSubscription> page = new Page<>(pn, ps);
        IPage<UserSubscription> result = page(page, Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getUserId, userId)
                .eq(StrUtil.isNotBlank(targetType), UserSubscription::getTargetType, targetType)
                .orderByDesc(UserSubscription::getCreateTime));

        List<SubscriptionVO> items = new ArrayList<>();
        for (UserSubscription sub : result.getRecords()) {
            SubscriptionVO vo = new SubscriptionVO();
            vo.setId(sub.getId());
            vo.setTargetType(sub.getTargetType());
            vo.setTargetId(sub.getTargetId());
            vo.setEventTypes(sub.getEventTypes());
            vo.setEnabled(sub.getEnabled());
            vo.setCreateTime(sub.getCreateTime());
            items.add(vo);
        }
        // 批量填充目标信息，避免N+1
        fillTargetsBatch(items);
        return PageResult.of(pn, ps, result.getTotal(), items);
    }

    @Override
    public List<Long> findSubscribers(String targetType, Long targetId, String eventType) {
        validateTargetType(targetType);
        if (StrUtil.isBlank(eventType)) {
            // 不指定事件类型，返回所有订阅者
            return list(Wrappers.<UserSubscription>lambdaQuery()
                    .eq(UserSubscription::getTargetType, targetType)
                    .eq(UserSubscription::getTargetId, targetId)
                    .eq(UserSubscription::getEnabled, 1))
                    .stream().map(UserSubscription::getUserId).collect(Collectors.toList());
        }
        // 指定事件类型，使用LIKE匹配（eventTypes是逗号分隔字符串）
        return list(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getTargetType, targetType)
                .eq(UserSubscription::getTargetId, targetId)
                .eq(UserSubscription::getEnabled, 1)
                .like(UserSubscription::getEventTypes, eventType))
                .stream().map(UserSubscription::getUserId).collect(Collectors.toList());
    }

    @Override
    public long count(Long userId, String targetType) {
        return count(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getUserId, userId)
                .eq(StrUtil.isNotBlank(targetType), UserSubscription::getTargetType, targetType));
    }

    @Override
    public UserSubscription getById(Long id) {
        return baseMapper.selectById(id);
    }

    // ==================== 私有方法 ====================

    /**
     * 构建订阅状态缓存key
     */
    private String buildCacheKey(Long userId, String targetType, Long targetId) {
        return Constants.CACHE_SUBSCRIPTION_PREFIX + userId + ":" + targetType + ":" + targetId;
    }

    /**
     * 清除订阅状态缓存
     */
    private void evictCache(Long userId, String targetType, Long targetId) {
        try {
            redisUtils.delete(buildCacheKey(userId, targetType, targetId));
        } catch (Exception e) {
            log.warn("Redis删除订阅缓存异常: {}", e.getMessage());
        }
    }

    /**
     * 校验订阅目标类型
     */
    private void validateTargetType(String targetType) {
        if (targetType == null || !VALID_TARGET_TYPES.contains(targetType.toUpperCase())) {
            throw new BusinessException("无效的订阅目标类型");
        }
    }

    /**
     * 校验订阅事件类型
     */
    private void validateEventTypes(String eventTypes) {
        if (StrUtil.isBlank(eventTypes)) {
            return;
        }
        String[] types = eventTypes.split(",");
        for (String type : types) {
            String trimmed = type.trim();
            if (!VALID_EVENT_TYPES.contains(trimmed)) {
                throw new BusinessException("无效的订阅事件类型: " + trimmed);
            }
        }
    }

    /**
     * 批量填充目标信息，避免N+1查询
     */
    private void fillTargetsBatch(List<SubscriptionVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        // 按类型分组收集ID
        Map<String, List<Long>> typeIds = new java.util.HashMap<>();
        for (SubscriptionVO vo : items) {
            if (vo.getTargetType() != null && vo.getTargetId() != null) {
                typeIds.computeIfAbsent(vo.getTargetType(), k -> new ArrayList<>()).add(vo.getTargetId());
            }
        }

        Map<Long, Robot> robotMap = new java.util.HashMap<>();
        Map<Long, Brand> brandMap = new java.util.HashMap<>();
        Map<Long, Company> companyMap = new java.util.HashMap<>();

        List<Long> robotIds = typeIds.getOrDefault(Constants.SUB_TARGET_ROBOT, Collections.emptyList());
        if (!robotIds.isEmpty()) {
            robotMapper.selectBatchIds(robotIds).forEach(r -> robotMap.put(r.getId(), r));
        }
        List<Long> brandIds = typeIds.getOrDefault(Constants.SUB_TARGET_BRAND, Collections.emptyList());
        if (!brandIds.isEmpty()) {
            brandMapper.selectBatchIds(brandIds).forEach(b -> brandMap.put(b.getId(), b));
        }
        List<Long> companyIds = typeIds.getOrDefault(Constants.SUB_TARGET_COMPANY, Collections.emptyList());
        if (!companyIds.isEmpty()) {
            companyMapper.selectBatchIds(companyIds).forEach(c -> companyMap.put(c.getId(), c));
        }

        // 填充
        for (SubscriptionVO vo : items) {
            String type = vo.getTargetType();
            Long id = vo.getTargetId();
            if (type == null || id == null) {
                continue;
            }
            switch (type) {
                case "ROBOT": {
                    Robot r = robotMap.get(id);
                    if (r != null) {
                        vo.setTargetName(r.getName());
                        vo.setTargetAvatar(r.getCoverImage());
                        vo.setTargetUrl("/robot/" + id);
                    }
                    break;
                }
                case "BRAND": {
                    Brand b = brandMap.get(id);
                    if (b != null) {
                        vo.setTargetName(b.getName());
                        vo.setTargetAvatar(b.getLogo());
                        vo.setTargetUrl("/brand/" + id);
                    }
                    break;
                }
                case "COMPANY": {
                    Company c = companyMap.get(id);
                    if (c != null) {
                        vo.setTargetName(c.getName());
                        vo.setTargetAvatar(c.getLogo());
                        vo.setTargetUrl("/company/" + id);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
}