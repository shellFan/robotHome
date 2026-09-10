package com.robot.home.behavior.service;

import com.robot.home.behavior.dto.BehaviorEventDTO;

import java.util.List;
import java.util.Map;

/**
 * 行为事件服务
 * <p>
 * 核心功能：
 * 1. 异步记录行为事件到 MySQL
 * 2. Redis ZINCRBY 实时更新热度分
 * 3. 支持事件类型白名单校验
 */
public interface BehaviorEventService {

    /** 事件类型白名单 */
    List<String> VALID_EVENT_TYPES = java.util.Arrays.asList(
            "VIEW", "SEARCH", "CLICK", "FAVORITE", "UNFAVORITE",
            "COMPARE", "INQUIRY", "SHARE", "COMMENT", "FOLLOW"
    );

    /** 业务类型白名单 */
    List<String> VALID_BIZ_TYPES = java.util.Arrays.asList(
            "robot", "article", "video", "post", "tutorial", "brand", "company"
    );

    /**
     * 记录行为事件（异步写入MySQL + Redis热度更新）
     *
     * @param userId     用户ID（可为空，匿名浏览）
     * @param dto        事件DTO
     * @param ip         请求IP
     * @param ua         User-Agent
     * @param sessionId  会话ID（匿名用户去重用）
     */
    void record(Long userId, BehaviorEventDTO dto, String ip, String ua, String sessionId);

    /**
     * 批量记录行为事件
     */
    void batchRecord(Long userId, List<BehaviorEventDTO> events, String ip, String ua, String sessionId);

    /**
     * 获取业务对象的热度分（从Redis ZSET读取）
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @return 热度分，不存在返回0
     */
    double getHotScore(String bizType, Long bizId);

    /**
     * 获取排行榜Top N（从Redis ZSET读取）
     *
     * @param bizType 业务类型
     * @param limit   数量
     * @return ID -> score 映射，按score降序
     */
    List<Map<String, Object>> getTopHot(String bizType, int limit);
}