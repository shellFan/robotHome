package com.robot.home.message.service;

import com.robot.home.common.PageResult;
import com.robot.home.message.vo.MessageVO;

/**
 * Phase10: 通知中心2.0服务
 * 升级而非重写MessageService，增加：
 * - 通知类型分类
 * - eventKey去重
 * - Redis未读数缓存
 * - 软删除
 * - 目标链接
 */
public interface NotificationService {

    /**
     * 发送通知（带去重）
     * @param userId 用户ID
     * @param notificationType 通知类型(FOLLOW_UPDATE/PARAM_CHANGE/PRICE_CHANGE/QUESTION_ANSWER/ANSWER_ACCEPTED/REVIEW_INTERACTION/POST_INTERACTION/PROCUREMENT_RESPONSE/SYSTEM)
     * @param title 标题
     * @param summary 摘要
     * @param targetType 目标类型(robot/brand/company/post/question/answer/review/procurement)
     * @param targetId 目标ID
     * @param eventKey 事件唯一键(SHA-256)，null则不去重
     */
    void sendNotification(Long userId, String notificationType, String title,
                          String summary, String targetType, Long targetId, String eventKey);

    /**
     * 批量发送通知给订阅者
     */
    void notifySubscribers(String targetType, Long targetId, String notificationType,
                           String title, String summary);

    /**
     * 通知列表（支持类型过滤）
     */
    PageResult<MessageVO> listNotifications(Long userId, String notificationType,
                                             Integer pageNum, Integer pageSize);

    /**
     * 未读数（Redis缓存+DB降级）
     */
    long unreadCount(Long userId);

    /**
     * 标记已读
     */
    void markRead(Long userId, Long id);

    /**
     * 全部标记已读
     */
    void markAllRead(Long userId);

    /**
     * 软删除
     */
    void deleteNotification(Long userId, Long id);

    /**
     * 按类型标记已读
     */
    void markReadByType(Long userId, String notificationType);

    /**
     * 点击通知（CTR追踪）
     * 标记通知为已点击，用于统计通知点击率
     */
    void clickNotification(Long userId, Long id);
}