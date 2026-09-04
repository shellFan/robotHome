package com.robot.home.message.service;

import com.robot.home.common.PageResult;
import com.robot.home.message.vo.MessageVO;

/**
 * 站内消息服务
 */
public interface MessageService {

    PageResult<MessageVO> list(Long userId, Integer pageNum, Integer pageSize);

    long unreadCount(Long userId);

    void markRead(Long userId, Long id);

    void markAllRead(Long userId);

    /**
     * 发送系统消息（内部使用）
     */
    void send(Long userId, String type, String title, String content, Long relatedId);
}
