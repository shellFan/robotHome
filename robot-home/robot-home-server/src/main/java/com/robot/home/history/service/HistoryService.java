package com.robot.home.history.service;

import com.robot.home.common.PageResult;
import com.robot.home.history.vo.HistoryItemVO;

/**
 * 浏览历史服务
 */
public interface HistoryService {

    /**
     * 记录浏览（同一业务对象保留最近一条）
     */
    void record(Long userId, String bizType, Long bizId);

    PageResult<HistoryItemVO> myHistory(Long userId, String bizType, Integer pageNum, Integer pageSize);

    void remove(Long userId, String bizType, Long bizId);

    void clear(Long userId, String bizType);

    /**
     * 浏览历史总数
     */
    long count(Long userId);
}
