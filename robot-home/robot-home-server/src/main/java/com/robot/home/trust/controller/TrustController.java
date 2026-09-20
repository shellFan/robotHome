package com.robot.home.trust.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.trust.service.TrustService;
import com.robot.home.trust.vo.RobotChangeRecordVO;
import com.robot.home.trust.vo.RobotDataSourceVO;
import com.robot.home.trust.vo.RobotTrustVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 信任体系公开接口（不需要登录）
 */
@RestController
@RequestMapping("/api/robots")
public class TrustController {

    @Resource
    private TrustService trustService;

    /**
     * 获取机器人可信度
     */
    @GetMapping("/{robotId}/trust")
    public Result<RobotTrustVO> getTrust(@PathVariable Long robotId) {
        return Result.success(trustService.getTrust(robotId));
    }

    /**
     * 获取数据来源列表
     */
    @GetMapping("/{robotId}/data-sources")
    public Result<List<RobotDataSourceVO>> getDataSources(@PathVariable Long robotId) {
        return Result.success(trustService.getDataSources(robotId));
    }

    /**
     * 变更历史（支持changeType筛选）
     */
    @GetMapping("/{robotId}/changes")
    public Result<PageResult<RobotChangeRecordVO>> getChangeHistory(
            @PathVariable Long robotId,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(trustService.getChangeHistory(robotId, changeType, pageNum, pageSize));
    }
}