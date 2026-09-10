package com.robot.home.behavior.controller;

import com.robot.home.behavior.dto.BehaviorEventDTO;
import com.robot.home.behavior.service.BehaviorEventService;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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

    @Resource
    private BehaviorEventService behaviorEventService;

    /**
     * 单条事件上报
     * POST /api/behavior/event
     */
    @PostMapping("/event")
    public Result<Void> record(@RequestBody BehaviorEventDTO dto, HttpServletRequest request) {
        Long userId = SecurityUtils.currentUserId();
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");
        behaviorEventService.record(userId, dto, ip, ua);
        return Result.success();
    }

    /**
     * 批量事件上报（前端批量埋点场景）
     * POST /api/behavior/batch
     */
    @PostMapping("/batch")
    public Result<Void> batchRecord(@RequestBody List<BehaviorEventDTO> events, HttpServletRequest request) {
        Long userId = SecurityUtils.currentUserId();
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");
        behaviorEventService.batchRecord(userId, events, ip, ua);
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
     * 获取客户端IP（兼容代理场景）
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
        return ip;
    }
}