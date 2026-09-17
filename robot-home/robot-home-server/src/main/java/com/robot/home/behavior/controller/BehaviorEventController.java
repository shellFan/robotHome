package com.robot.home.behavior.controller;

import com.robot.home.behavior.dto.BehaviorEventDTO;
import com.robot.home.behavior.service.BehaviorEventService;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.ratelimit.annotation.RateLimit;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 行为事件 API
 * <p>
 * 前端埋点上报用户行为（浏览/收藏/对比/询价等），后端记录事件并更新热度
 */
@RestController
@RequestMapping("/api/behavior")
public class BehaviorEventController {

    /** 批量上报最大条数 */
    private static final int BATCH_MAX_SIZE = 100;

    @Resource
    private BehaviorEventService behaviorEventService;

    /**
     * 单条事件上报
     * POST /api/behavior/event
     */
    @PostMapping("/event")
    @RateLimit(action = "behavior", windowSeconds = 60, maxRequests = 120, dimension = "IP_USER")
    public Result<Void> record(@RequestBody @Valid BehaviorEventDTO dto, HttpServletRequest request) {
        Long userId = SecurityUtils.currentUserId();
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");
        String sessionId = getSessionId(request);
        behaviorEventService.record(userId, dto, ip, ua, sessionId);
        return Result.success();
    }

    /**
     * 批量事件上报（前端批量埋点场景，最多100条）
     * POST /api/behavior/batch
     */
    @PostMapping("/batch")
    @RateLimit(action = "behavior_batch", windowSeconds = 60, maxRequests = 10, dimension = "IP_USER")
    public Result<Void> batchRecord(@RequestBody List<@Valid BehaviorEventDTO> events, HttpServletRequest request) {
        if (events == null || events.isEmpty()) {
            return Result.success();
        }
        if (events.size() > BATCH_MAX_SIZE) {
            throw new BusinessException("批量上报不能超过" + BATCH_MAX_SIZE + "条");
        }
        Long userId = SecurityUtils.currentUserId();
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");
        String sessionId = getSessionId(request);
        behaviorEventService.batchRecord(userId, events, ip, ua, sessionId);
        return Result.success();
    }

    /**
     * 查询对象热度分
     * GET /api/behavior/hot?bizType=robot&bizId=123
     */
    @GetMapping("/hot/score")
    public Result<Double> getHotScore(@RequestParam String bizType, @RequestParam Long bizId) {
        double score = behaviorEventService.getHotScore(bizType, bizId);
        return Result.success(score);
    }

    /**
     * 查询热度排行榜 Top N
     * GET /api/behavior/hot/top?bizType=robot&limit=20
     */
    @GetMapping("/hot/top")
    public Result<List<Map<String, Object>>> getTopHot(@RequestParam String bizType,
                                                        @RequestParam(defaultValue = "20") Integer limit) {
        List<Map<String, Object>> top = behaviorEventService.getTopHot(bizType, limit);
        return Result.success(top);
    }

    /**
     * 获取客户端IP（兼容代理场景，截取前64字符防注入）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // 截取前64字符防注入
        if (ip != null && ip.length() > 64) {
            ip = ip.substring(0, 64);
        }
        return ip;
    }

    /**
     * 获取会话ID（匿名用户去重用）
     * 优先从Header获取，否则从Cookie的JSESSIONID获取
     */
    private String getSessionId(HttpServletRequest request) {
        String sessionId = request.getHeader("X-Session-Id");
        if (sessionId != null && !sessionId.isEmpty()) {
            return sessionId;
        }
        sessionId = request.getRequestedSessionId();
        return sessionId != null ? sessionId : "anon-" + getClientIp(request);
    }
}