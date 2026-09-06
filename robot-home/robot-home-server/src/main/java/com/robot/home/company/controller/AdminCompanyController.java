package com.robot.home.company.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.company.dto.CompanyDTO;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.security.RequirePermission;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 后台企业管理
 */
@RestController
@RequestMapping("/api/admin/companies")
public class AdminCompanyController {

    @Resource
    private CompanyMapper companyMapper;

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
    public Result<Long> save(@RequestBody CompanyDTO dto) {
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
        return Result.success(entity.getId());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("company:delete")
    public Result<Void> delete(@PathVariable Long id) {
        companyMapper.deleteById(id);
        return Result.success();
    }
}
