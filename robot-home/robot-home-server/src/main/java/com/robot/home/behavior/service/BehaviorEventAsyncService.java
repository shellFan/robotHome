package com.robot.home.behavior.service;

import com.robot.home.behavior.dto.BehaviorEventDTO;

/**
 * 行为事件异步服务
 * <p>
 * 独立Bean，解决Spring @Async自调用不生效问题。
 * BehaviorEventServiceImpl通过Spring代理调用此Service，确保@Async生效。
 */
public interface BehaviorEventAsyncService {

    /**
     * 异步写入MySQL
     *
     * @param userId 用户ID
     * @param dto    事件DTO
     * @param ip     请求IP
     * @param ua     User-Agent
     */
    void saveEvent(Long userId, BehaviorEventDTO dto, String ip, String ua);
}