package com.robot.home.banner.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.banner.entity.Banner;
import com.robot.home.banner.mapper.BannerMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.security.RequirePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * 后台 Banner 管理
 */
@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    private static final Logger log = LoggerFactory.getLogger(AdminBannerController.class);

    @Resource
    private BannerMapper bannerMapper;
    @Resource
    private RedisUtils redisUtils;

    @GetMapping
    @RequirePermission("operation:banner")
    public Result<List<Banner>> list(@RequestParam(required = false) String position) {
        return Result.success(bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                .eq(StrUtil.isNotBlank(position), Banner::getPosition, position)
                .orderByAsc(Banner::getSort)));
    }

    @GetMapping("/{id}")
    @RequirePermission("operation:banner")
    public Result<Banner> detail(@PathVariable Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException("Banner 不存在");
        }
        return Result.success(banner);
    }

    @PostMapping
    @RequirePermission("operation:banner:edit")
    public Result<Long> save(@RequestBody Banner banner) {
        if (StrUtil.isBlank(banner.getImage())) {
            throw new BusinessException("请上传 Banner 图片");
        }
        if (StrUtil.isBlank(banner.getPosition())) {
            banner.setPosition("pc");
        }
        if (banner.getId() == null) {
            if (banner.getStatus() == null) {
                banner.setStatus(1);
            }
            bannerMapper.insert(banner);
        } else {
            if (bannerMapper.selectById(banner.getId()) == null) {
                throw new BusinessException("Banner 不存在");
            }
            bannerMapper.updateById(banner);
        }
        clearCache();
        return Result.success(banner.getId());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("operation:banner:edit")
    public Result<Void> delete(@PathVariable Long id) {
        bannerMapper.deleteById(id);
        clearCache();
        return Result.success();
    }

    /**
     * 清理 Banner 缓存，保证后台修改即时生效
     */
    private void clearCache() {
        try {
            Set<String> keys = redisUtils.keys(Constants.CACHE_BANNER_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis Banner缓存清空失败: error={}", e.getMessage());
        }
    }
}
