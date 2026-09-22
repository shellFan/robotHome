package com.robot.home.subscription.service;

import com.robot.home.common.PageResult;
import com.robot.home.subscription.vo.SubscriptionVO;

import java.util.List;
import java.util.Set;

/**
 * 订阅服务：ROBOT / BRAND / COMPANY
 */
public interface SubscriptionService {

    /**
     * 检查是否已订阅
     */
    boolean checkSubscribed(Long userId, String targetType, Long targetId);

    /**
     * 批量检查订阅状态，返回已订阅的 targetId 集合
     */
    Set<Long> checkBatchSubscribed(Long userId, String targetType, List<Long> targetIds);

    /**
     * 切换订阅状态（幂等），返回切换后是否订阅
     */
    boolean toggle(Long userId, String targetType, Long targetId);

    /**
     * 更新订阅事件类型
     */
    void updateEventTypes(Long userId, String targetType, Long targetId, String eventTypes);

    /**
     * 我的订阅列表
     */
    PageResult<SubscriptionVO> mySubscriptions(Long userId, String targetType, Integer pageNum, Integer pageSize);

    /**
     * 查找某目标的订阅者userId列表（用于通知推送）
     */
    List<Long> findSubscribers(String targetType, Long targetId, String eventType);

    /**
     * 计数
     */
    long count(Long userId, String targetType);

    /**
     * 根据ID获取订阅记录
     */
    com.robot.home.subscription.entity.UserSubscription getById(Long id);
}