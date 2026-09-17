package com.robot.home.company.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.dto.CompanyDTO;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.security.RequirePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * 后台企业管理
 */
@RestController
@RequestMapping("/api/admin/companies")
public class AdminCompanyController {

    private static final Logger log = LoggerFactory.getLogger(AdminCompanyController.class);

    @Resource
    private CompanyMapper companyMapper;

    @Resource
    private BrandMapper brandMapper;

    @Resource
    private RedisUtils redisUtils;

    @GetMapping
    @RequirePermission("company:list")
    public Result<PageResult<Company>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String region,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Company> page = new Page<>(pn, ps);
        IPage<Company> result = companyMapper.selectPage(page, Wrappers.<Company>lambdaQuery()
                .likeRight(StrUtil.isNotBlank(keyword), Company::getName, keyword)
                .eq(StrUtil.isNotBlank(region), Company::getRegion, region)
                .orderByDesc(Company::getHotScore));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/all")
    @RequirePermission("company:list")
    public Result<List<Company>> all() {
        return Result.success(companyMapper.selectList(Wrappers.<Company>lambdaQuery()
                .eq(Company::getStatus, 1).orderByDesc(Company::getHotScore)));
    }

    @GetMapping("/{id}")
    @RequirePermission("company:list")
    public Result<Company> detail(@PathVariable Long id) {
        Company company = companyMapper.selectById(id);
        if (company == null) {
            throw new BusinessException("企业不存在");
        }
        return Result.success(company);
    }

    @PostMapping
    @RequirePermission("company:add")
    public Result<Long> save(@RequestBody @Valid CompanyDTO dto) {
        if (StrUtil.isBlank(dto.getName())) {
            throw new BusinessException("企业名称不能为空");
        }
        Company entity = new Company();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getId() == null) {
            if (entity.getStatus() == null) {
                entity.setStatus(1);
            }
            companyMapper.insert(entity);
        } else {
            if (companyMapper.selectById(entity.getId()) == null) {
                throw new BusinessException("企业不存在");
            }
            companyMapper.updateById(entity);
        }
        clearCache();
        return Result.success(entity.getId());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("company:delete")
    public Result<Void> delete(@PathVariable Long id) {
        long brandCount = brandMapper.selectCount(Wrappers.<Brand>lambdaQuery().eq(Brand::getCompanyId, id));
        if (brandCount > 0) {
            throw new BusinessException("该企业下仍有 " + brandCount + " 个品牌，不能删除");
        }
        companyMapper.deleteById(id);
        clearCache();
        return Result.success();
    }

    /**
     * 清理企业缓存，保证后台修改即时生效
     */
    private void clearCache() {
        try {
            Set<String> keys = redisUtils.keys(Constants.CACHE_COMPANY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis企业缓存清空失败: error={}", e.getMessage());
        }
    }
}
