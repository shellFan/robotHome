package com.robot.home.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.message.entity.Message;
import com.robot.home.message.mapper.MessageMapper;
import com.robot.home.message.service.NotificationService;
import com.robot.home.message.vo.MessageVO;
import com.robot.home.subscription.entity.UserSubscription;
import com.robot.home.subscription.mapper.UserSubscriptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Phase10: 通知中心2.0实现
 * 升级MessageService，增加通知类型、去重、Redis缓存、软删除
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final long UNREAD_CACHE_SECONDS = 120L;

    /** 允许的通知类型白名单 */
    private static final Set<String> VALID_NOTIFICATION_TYPES = new HashSet<>(Arrays.asList(
            Constants.NOTIFY_SYSTEM,
            Constants.NOTIFY_FOLLOW_UPDATE,
            Constants.NOTIFY_PARAM_CHANGE,
            Constants.NOTIFY_PRICE_CHANGE,
            Constants.NOTIFY_QUESTION_ANSWER,
            Constants.NOTIFY_ANSWER_ACCEPTED,
            Constants.NOTIFY_REVIEW_INTERACTION,
            Constants.NOTIFY_POST_INTERACTION,
            Constants.NOTIFY_PROCUREMENT_RESPONSE
    ));

    @Resource
    private MessageMapper messageMapper;
    @Resource
    private UserSubscriptionMapper userSubscriptionMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long userId, String notificationType, String title,
                                  String summary, String targetType, Long targetId, String eventKey) {
        if (userId == null || StrUtil.isBlank(title)) {
            return;
        }
        // 类型白名单校验
        if (!VALID_NOTIFICATION_TYPES.contains(notificationType)) {
            notificationType = Constants.NOTIFY_SYSTEM;
        }
        // eventKey去重
        if (StrUtil.isNotBlank(eventKey)) {
            Long exists = messageMapper.selectCount(Wrappers.<Message>lambdaQuery()
                    .eq(Message::getEventKey, eventKey)
                    .eq(Message::getUserId, userId));
            if (exists != null && exists > 0) {
                log.debug("Notification dedup: eventKey={}, userId={}", eventKey, userId);
                return;
            }
        }

        Message message = new Message();
        message.setUserId(userId);
        message.setType("notification");
        message.setNotificationType(notificationType);
        message.setTitle(title);
        message.setContent(summary);
        message.setSummary(summary);
        message.setTargetType(targetType);
        message.setTargetId(targetId);
        message.setEventKey(eventKey);
        message.setIsRead(0);
        message.setDeleted(0);
        message.setCreateTime(LocalDateTime.now());

        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException e) {
            // eventKey唯一索引兜底
            log.debug("Notification dedup by unique key: eventKey={}", eventKey);
            return;
        }

        // 清除未读数缓存
        invalidateUnreadCache(userId);
    }

    @Override
    public void notifySubscribers(String targetType, Long targetId, String notificationType,
                                   String title, String summary) {
        // 查找订阅了该目标的用户
        List<UserSubscription> subs = userSubscriptionMapper.selectList(
                Wrappers.<UserSubscription>lambdaQuery()
                        .eq(UserSubscription::getTargetType, targetType)
                        .eq(UserSubscription::getTargetId, targetId)
                        .eq(UserSubscription::getEnabled, 1));
        if (subs == null || subs.isEmpty()) {
            return;
        }

        // 检查订阅者是否订阅了该事件类型
        for (UserSubscription sub : subs) {
            String eventTypes = sub.getEventTypes();
            if (StrUtil.isNotBlank(eventTypes) && eventTypes.contains(notificationType)) {
                try {
                    String eventKey = "sub_" + notificationType + "_" + targetType + "_" + targetId + "_" + sub.getUserId();
                    sendNotification(sub.getUserId(), notificationType, title, summary,
                            targetType.toLowerCase(), targetId, sha256(eventKey));
                } catch (Exception e) {
                    log.warn("Failed to notify subscriber: userId={}, error={}", sub.getUserId(), e.getMessage());
                }
            }
        }
    }

    @Override
    public PageResult<MessageVO> listNotifications(Long userId, String notificationType,
                                                     Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Message> page = new Page<>(pn, ps);

        IPage<Message> result = messageMapper.selectPage(page, Wrappers.<Message>lambdaQuery()
                .eq(Message::getUserId, userId)
                .eq(Message::getDeleted, 0)
                .eq(StrUtil.isNotBlank(notificationType), Message::getNotificationType, notificationType)
                .orderByDesc(Message::getCreateTime));

        List<MessageVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public long unreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        // Redis缓存
        String cacheKey = Constants.CACHE_NOTIFICATION_UNREAD_PREFIX + userId;
        try {
            Object cached = redisUtils.get(cacheKey);
            if (cached != null) {
                return Long.parseLong(cached.toString());
            }
        } catch (Exception e) {
            log.warn("Redis unread count cache read failed, fallback to DB: {}", e.getMessage());
        }

        long count = messageMapper.selectCount(Wrappers.<Message>lambdaQuery()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0)
                .eq(Message::getDeleted, 0));

        // 写入缓存
        try {
            redisUtils.set(cacheKey, String.valueOf(count), UNREAD_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis unread count cache write failed: {}", e.getMessage());
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long id) {
        if (userId == null || id == null) {
            return;
        }
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getId, id)
                .eq(Message::getUserId, userId)
                .eq(Message::getDeleted, 0)
                .set(Message::getIsRead, 1));
        invalidateUnreadCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        if (userId == null) {
            return;
        }
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0)
                .eq(Message::getDeleted, 0)
                .set(Message::getIsRead, 1));
        invalidateUnreadCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long userId, Long id) {
        if (userId == null || id == null) {
            return;
        }
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getId, id)
                .eq(Message::getUserId, userId)
                .set(Message::getDeleted, 1));
        invalidateUnreadCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReadByType(Long userId, String notificationType) {
        if (userId == null || StrUtil.isBlank(notificationType)) {
            return;
        }
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getUserId, userId)
                .eq(Message::getNotificationType, notificationType)
                .eq(Message::getIsRead, 0)
                .eq(Message::getDeleted, 0)
                .set(Message::getIsRead, 1));
        invalidateUnreadCache(userId);
    }

    /**
     * 清除未读数缓存
     */
    private void invalidateUnreadCache(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisUtils.delete(Constants.CACHE_NOTIFICATION_UNREAD_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis unread cache delete failed: {}", e.getMessage());
        }
    }

    private MessageVO toVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setType(m.getType());
        vo.setTitle(m.getTitle());
        vo.setContent(m.getContent());
        vo.setRelatedId(m.getRelatedId());
        vo.setIsRead(m.getIsRead());
        vo.setCreateTime(m.getCreateTime());
        // Phase10 新增字段
        vo.setNotificationType(m.getNotificationType());
        vo.setTargetType(m.getTargetType());
        vo.setTargetId(m.getTargetId());
        vo.setSummary(m.getSummary());
        return vo;
    }

    /**
     * SHA-256哈希
     */
    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}