package com.robot.home.recommend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.Constants;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.recommend.entity.RecommendItem;
import com.robot.home.recommend.entity.RecommendPosition;
import com.robot.home.recommend.mapper.RecommendItemMapper;
import com.robot.home.recommend.mapper.RecommendPositionMapper;
import com.robot.home.security.RequirePermission;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * 后台推荐位管理
 */
@RestController
@RequestMapping("/api/admin/recommends")
public class AdminRecommendController {

    @Resource
    private RecommendPositionMapper positionMapper;
    @Resource
    private RecommendItemMapper itemMapper;
    @Resource
    private RedisUtils redisUtils;

    @GetMapping("/positions")
    @RequirePermission("operation:recommend")
    public Result<List<RecommendPosition>> positions() {
        return Result.success(positionMapper.selectList(Wrappers.<RecommendPosition>lambdaQuery()
                .orderByAsc(RecommendPosition::getId)));
    }

    @PostMapping("/positions")
    @RequirePermission("operation:recommend:edit")
    public Result<Long> savePosition(@RequestBody RecommendPosition position) {
        if (StrUtil.isBlank(position.getCode()) || StrUtil.isBlank(position.getName())) {
            throw new BusinessException("推荐位编码与名称不能为空");
        }
        if (position.getId() == null) {
            if (position.getStatus() == null) {
                position.setStatus(1);
            }
            positionMapper.insert(position);
        } else {
            positionMapper.updateById(position);
        }
        clearCache();
        return Result.success(position.getId());
    }

    @GetMapping("/items")
    @RequirePermission("operation:recommend")
    public Result<List<RecommendItem>> items(@RequestParam(required = false) Long positionId) {
        return Result.success(itemMapper.selectList(Wrappers.<RecommendItem>lambdaQuery()
                .eq(positionId != null, RecommendItem::getPositionId, positionId)
                .orderByAsc(RecommendItem::getSort)));
    }

    @PostMapping("/items")
    @RequirePermission("operation:recommend:edit")
    public Result<Long> saveItem(@RequestBody RecommendItem item) {
        if (item.getPositionId() == null) {
            throw new BusinessException("请选择推荐位");
        }
        if (item.getId() == null) {
            if (item.getStatus() == null) {
                item.setStatus(1);
            }
            itemMapper.insert(item);
        } else {
            if (itemMapper.selectById(item.getId()) == null) {
                throw new BusinessException("推荐项不存在");
            }
            itemMapper.updateById(item);
        }
        clearCache();
        return Result.success(item.getId());
    }

    @DeleteMapping("/items/{id}")
    @RequirePermission("operation:recommend:edit")
    public Result<Void> deleteItem(@PathVariable Long id) {
        itemMapper.deleteById(id);
        clearCache();
        return Result.success();
    }

    private void clearCache() {
        Set<String> keys = redisUtils.keys(Constants.CACHE_RECOMMEND_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisUtils.delete(keys);
        }
    }
}
