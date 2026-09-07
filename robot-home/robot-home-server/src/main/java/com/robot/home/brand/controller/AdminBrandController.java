package com.robot.home.brand.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.brand.dto.BrandDTO;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.security.RequirePermission;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 后台品牌管理
 */
@RestController
@RequestMapping("/api/admin/brands")
public class AdminBrandController {

    @Resource
    private BrandMapper brandMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RedisUtils redisUtils;

    @GetMapping
    @RequirePermission("brand:list")
    public Result<PageResult<Brand>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Brand> page = new Page<>(pn, ps);
        IPage<Brand> result = brandMapper.selectPage(page, Wrappers.<Brand>lambdaQuery()
                .likeRight(StrUtil.isNotBlank(keyword), Brand::getName, keyword)
                .eq(status != null, Brand::getStatus, status)
                .orderByDesc(Brand::getHotScore));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/all")
    @RequirePermission("brand:list")
    public Result<java.util.List<Brand>> all() {
        return Result.success(brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1).orderByDesc(Brand::getHotScore)));
    }

    @GetMapping("/{id}")
    @RequirePermission("brand:list")
    public Result<Brand> detail(@PathVariable Long id) {
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new BusinessException("品牌不存在");
        }
        return Result.success(brand);
    }

    @PostMapping
    @RequirePermission("brand:add")
    public Result<Long> save(@RequestBody @Valid BrandDTO dto) {
        if (StrUtil.isBlank(dto.getName())) {
            throw new BusinessException("品牌名称不能为空");
        }
        Brand entity = new Brand();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getInitial() == null && StrUtil.isNotBlank(entity.getName())) {
            entity.setInitial(entity.getName().substring(0, 1).toUpperCase());
        }
        if (entity.getId() == null) {
            if (entity.getStatus() == null) {
                entity.setStatus(1);
            }
            brandMapper.insert(entity);
        } else {
            if (brandMapper.selectById(entity.getId()) == null) {
                throw new BusinessException("品牌不存在");
            }
            brandMapper.updateById(entity);
        }
        refreshRobotCount(entity.getId());
        clearBrandCache();
        return Result.success(entity.getId());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("brand:delete")
    public Result<Void> delete(@PathVariable Long id) {
        long count = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery().eq(Robot::getBrandId, id));
        if (count > 0) {
            throw new BusinessException("该品牌下仍有 " + count + " 台机器人，不能删除");
        }
        brandMapper.deleteById(id);
        clearBrandCache();
        return Result.success();
    }

    @PostMapping("/{id}/status")
    @RequirePermission("brand:edit")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setStatus(status);
        brandMapper.updateById(brand);
        clearBrandCache();
        return Result.success();
    }

    private void refreshRobotCount(Long brandId) {
        long count = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery().eq(Robot::getBrandId, brandId));
        Brand update = new Brand();
        update.setId(brandId);
        update.setRobotCount((int) count);
        brandMapper.updateById(update);
    }

    private void clearBrandCache() {
        for (String key : redisUtils.keys(Constants.CACHE_BRAND_PREFIX + "*")) {
            redisUtils.delete(key);
        }
    }
}
