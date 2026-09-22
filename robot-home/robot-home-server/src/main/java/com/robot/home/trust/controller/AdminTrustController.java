package com.robot.home.trust.controller;

import cn.hutool.core.util.StrUtil;
import com.robot.home.common.Result;
import com.robot.home.security.RequirePermission;
import com.robot.home.trust.entity.RobotDataSource;
import com.robot.home.trust.mapper.RobotDataSourceMapper;
import com.robot.home.trust.service.TrustService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 信任体系管理接口（需要权限）
 */
@RestController
@RequestMapping("/api/admin/robots")
public class AdminTrustController {

    @Resource
    private TrustService trustService;
    @Resource
    private RobotDataSourceMapper dataSourceMapper;

    /**
     * 重算信任分
     */
    @PostMapping("/{robotId}/trust/recalculate")
    @RequirePermission("robot:edit")
    public Result<Void> recalculateTrust(@PathVariable Long robotId) {
        trustService.recalculateTrust(robotId);
        return Result.success();
    }

    /**
     * 添加数据来源
     */
    @PostMapping("/{robotId}/data-sources")
    @RequirePermission("robot:edit")
    public Result<Void> addDataSource(@PathVariable Long robotId,
                                       @RequestParam String sourceType,
                                       @RequestParam String sourceName,
                                       @RequestParam(required = false) String sourceUrl) {
        trustService.addDataSource(robotId, sourceType, StrUtil.trim(sourceName), StrUtil.trim(sourceUrl));
        return Result.success();
    }

    /**
     * 验证来源
     */
    @PostMapping("/{robotId}/data-sources/{sourceId}/verify")
    @RequirePermission("robot:edit")
    public Result<Void> verifyDataSource(@PathVariable Long robotId,
                                          @PathVariable Long sourceId) {
        RobotDataSource ds = dataSourceMapper.selectOne(
                Wrappers.<RobotDataSource>lambdaQuery()
                        .eq(RobotDataSource::getId, sourceId)
                        .eq(RobotDataSource::getRobotId, robotId));
        if (ds == null) {
            return Result.error(Result.CODE_NOT_FOUND, "数据来源不存在");
        }
        ds.setVerified(1);
        ds.setVerifiedTime(LocalDateTime.now());
        dataSourceMapper.updateById(ds);

        // 验证后重算信任分
        trustService.recalculateTrust(robotId);
        return Result.success();
    }
}