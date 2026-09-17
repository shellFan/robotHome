package com.robot.home.selection.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.selection.dto.SelectionSearchDTO;
import com.robot.home.selection.service.RobotSelectionService;
import com.robot.home.selection.vo.SelectionResultVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 机器人选型
 */
@RestController
@RequestMapping("/api/robot-selection")
public class RobotSelectionController {

    @Resource
    private RobotSelectionService selectionService;

    @PostMapping("/search")
    @RateLimit(action = "selection_search", windowSeconds = 60, maxRequests = 20, dimension = "IP_USER")
    public Result<PageResult<SelectionResultVO>> search(@RequestBody SelectionSearchDTO dto) {
        dto.setUserId(SecurityUtils.currentUserId());
        return Result.success(selectionService.search(dto));
    }

    @GetMapping("/filters")
    public Result<Map<String, Object>> getFilters(
            @RequestParam(required = false) String category) {
        return Result.success(selectionService.getFilters(category));
    }
}