package com.robot.home.correction.controller;

import cn.hutool.core.util.StrUtil;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.correction.service.RobotParamCorrectionService;
import com.robot.home.correction.vo.ParamCorrectionVO;
import com.robot.home.security.RequirePermission;
import com.robot.home.security.UserContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 后台：参数纠错审核管理
 */
@RestController
@RequestMapping("/api/admin/corrections")
public class AdminRobotParamCorrectionController {

    @Resource
    private RobotParamCorrectionService correctionService;

    /**
     * 分页列表（可按状态/robotId筛选）
     */
    @GetMapping
    @RequirePermission("correction:list")
    public Result<PageResult<ParamCorrectionVO>> page(@RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) Long robotId,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(correctionService.adminPage(status, robotId, pageNum, pageSize));
    }

    /**
     * 审核纠错（采纳/拒绝）
     * status: 1=采纳 2=拒绝
     */
    @PostMapping("/{id}/audit")
    @RequirePermission("correction:audit")
    public Result<Void> audit(@PathVariable Long id,
                               @RequestParam Integer status,
                               @RequestParam(required = false) String reviewNote) {
        Long reviewerId = UserContext.getUserId();
        correctionService.audit(reviewerId, id, status, StrUtil.trim(reviewNote));
        return Result.success();
    }
}