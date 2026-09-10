package com.robot.home.ranking.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.PageUtils;
import com.robot.home.ranking.entity.RankingDecayConfig;
import com.robot.home.ranking.entity.RankingSnapshot;
import com.robot.home.ranking.entity.RankingWeight;
import com.robot.home.ranking.mapper.RankingDecayConfigMapper;
import com.robot.home.ranking.mapper.RankingSnapshotMapper;
import com.robot.home.ranking.mapper.RankingWeightMapper;
import com.robot.home.ranking.service.impl.RankingServiceImpl;
import com.robot.home.security.RequirePermission;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 后台排行榜管理：权重配置 / 衰减配置 / 快照查看 / 手动刷新
 */
@RestController
@RequestMapping("/api/admin/rankings")
public class AdminRankingController {

    @Resource
    private RankingWeightMapper rankingWeightMapper;
    @Resource
    private RankingDecayConfigMapper rankingDecayConfigMapper;
    @Resource
    private RankingSnapshotMapper rankingSnapshotMapper;
    @Resource
    private RankingServiceImpl rankingServiceImpl;

    // ==================== 权重配置 ====================

    /**
     * 查看所有权重配置
     */
    @GetMapping("/weights")
    @RequirePermission("ranking:list")
    public Result<List<RankingWeight>> listWeights() {
        return Result.success(rankingWeightMapper.selectList(
                Wrappers.<RankingWeight>lambdaQuery().orderByAsc(RankingWeight::getId)));
    }

    /**
     * 更新权重配置
     */
    @PutMapping("/weights/{id}")
    @RequirePermission("ranking:edit")
    public Result<Void> updateWeight(@PathVariable Long id, @RequestBody RankingWeight weight) {
        RankingWeight existing = rankingWeightMapper.selectById(id);
        if (existing == null) {
            return Result.fail("权重配置不存在");
        }
        weight.setId(id);
        rankingWeightMapper.updateById(weight);
        // 刷新内存缓存
        rankingServiceImpl.refreshWeightCache();
        return Result.success();
    }

    /**
     * 新增权重配置
     */
    @PostMapping("/weights")
    @RequirePermission("ranking:edit")
    public Result<Long> addWeight(@RequestBody RankingWeight weight) {
        if (StrUtil.isBlank(weight.getEventType()) || weight.getWeight() == null) {
            return Result.fail("事件类型和权重不能为空");
        }
        rankingWeightMapper.insert(weight);
        rankingServiceImpl.refreshWeightCache();
        return Result.success(weight.getId());
    }

    // ==================== 衰减配置 ====================

    /**
     * 查看所有衰减配置
     */
    @GetMapping("/decay-configs")
    @RequirePermission("ranking:list")
    public Result<List<RankingDecayConfig>> listDecayConfigs() {
        return Result.success(rankingDecayConfigMapper.selectList(
                Wrappers.<RankingDecayConfig>lambdaQuery().orderByAsc(RankingDecayConfig::getId)));
    }

    /**
     * 更新衰减配置
     */
    @PutMapping("/decay-configs/{id}")
    @RequirePermission("ranking:edit")
    public Result<Void> updateDecayConfig(@PathVariable Long id, @RequestBody RankingDecayConfig config) {
        RankingDecayConfig existing = rankingDecayConfigMapper.selectById(id);
        if (existing == null) {
            return Result.fail("衰减配置不存在");
        }
        config.setId(id);
        rankingDecayConfigMapper.updateById(config);
        rankingServiceImpl.refreshWeightCache();
        return Result.success();
    }

    // ==================== 快照查看 ====================

    /**
     * 查看排行榜快照（分页）
     */
    @GetMapping("/snapshots")
    @RequirePermission("ranking:list")
    public Result<PageResult<RankingSnapshot>> listSnapshots(
            @RequestParam(required = false) String rankType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RankingSnapshot> page = new Page<>(pn, ps);
        IPage<RankingSnapshot> result = rankingSnapshotMapper.selectPage(page,
                Wrappers.<RankingSnapshot>lambdaQuery()
                        .eq(StrUtil.isNotBlank(rankType), RankingSnapshot::getRankType, rankType)
                        .orderByDesc(RankingSnapshot::getSnapshotDate)
                        .orderByAsc(RankingSnapshot::getRankNo));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    // ==================== 手动操作 ====================

    /**
     * 手动刷新热度分
     */
    @PostMapping("/refresh")
    @RequirePermission("ranking:edit")
    public Result<Integer> refreshHotScores() {
        int count = rankingServiceImpl.refreshHotScores();
        return Result.success(count);
    }

    /**
     * 手动触发排行榜快照
     */
    @PostMapping("/snapshot")
    @RequirePermission("ranking:edit")
    public Result<Integer> snapshotRankings() {
        int count = rankingServiceImpl.snapshotRankings();
        return Result.success(count);
    }

    /**
     * 查看当前权重配置（内存缓存）
     */
    @GetMapping("/current-weights")
    @RequirePermission("ranking:list")
    public Result<Map<String, Integer>> currentWeights() {
        return Result.success(rankingServiceImpl.getWeightConfig());
    }
}