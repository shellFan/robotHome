package com.robot.home.history.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.history.service.HistoryService;
import com.robot.home.history.vo.HistoryItemVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 浏览历史
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Resource
    private HistoryService historyService;

    @GetMapping
    public Result<PageResult<HistoryItemVO>> list(@RequestParam(required = false) String bizType,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(historyService.myHistory(userId, bizType, pageNum, pageSize));
    }

    @DeleteMapping("/{bizType}/{bizId}")
    public Result<Void> remove(@PathVariable String bizType, @PathVariable Long bizId) {
        Long userId = SecurityUtils.requireUserId();
        historyService.remove(userId, bizType, bizId);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> clear(@RequestParam(required = false) String bizType) {
        Long userId = SecurityUtils.requireUserId();
        historyService.clear(userId, bizType);
        return Result.success();
    }
}
