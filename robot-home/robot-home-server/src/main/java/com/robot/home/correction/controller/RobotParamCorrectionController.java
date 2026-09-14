package com.robot.home.correction.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.correction.dto.ParamCorrectionDTO;
import com.robot.home.correction.service.RobotParamCorrectionService;
import com.robot.home.correction.vo.ParamCorrectionVO;
import com.robot.home.ratelimit.annotation.RateLimit;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

/**
 * C端：参数纠错
 */
@RestController
@RequestMapping("/api/corrections")
public class RobotParamCorrectionController {

    @Resource
    private RobotParamCorrectionService correctionService;

    /**
     * 提交纠错
     */
    @PostMapping
    @RateLimit(action = "correction", windowSeconds = 60, maxRequests = 5, dimension = "IP_USER")
    public Result<Map<String, Object>> submit(@RequestBody @Valid ParamCorrectionDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        Long id = correctionService.submit(userId, dto);
        return Result.success(Collections.singletonMap("id", id));
    }

    /**
     * 我的纠错列表
     */
    @GetMapping("/my")
    public Result<PageResult<ParamCorrectionVO>> myCorrections(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(correctionService.myCorrections(userId, pageNum, pageSize));
    }
}