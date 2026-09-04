package com.robot.home.company.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.JsonUtils;
import com.robot.home.common.util.PageUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.service.CompanyService;
import com.robot.home.company.vo.CompanyDetailVO;
import com.robot.home.company.vo.CompanyListVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.vo.RobotSummaryVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 企业服务实现
 */
@Service
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, Company> implements CompanyService {

    @Resource
    private BrandMapper brandMapper;
    @Resource
    private RobotMapper robotMapper;

    @Override
    public PageResult<CompanyListVO> page(String keyword, String region, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Company> page = new Page<>(pn, ps);
        IPage<Company> result = page(page, Wrappers.<Company>lambdaQuery()
                .eq(Company::getStatus, 1)
                .eq(StrUtil.isNotBlank(region), Company::getRegion, region)
                .and(StrUtil.isNotBlank(keyword), w -> w.like(Company::getName, keyword).or().like(Company::getIntro, keyword))
                .orderByDesc(Company::getHotScore));
        List<CompanyListVO> vos = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public CompanyDetailVO detail(Long id) {
        Company company = getById(id);
        if (company == null) {
            throw new BusinessException("企业不存在");
        }
        CompanyDetailVO vo = new CompanyDetailVO();
        vo.setCompany(company);
        vo.setBrandList(brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getCompanyId, id)
                .eq(Brand::getStatus, 1)
                .orderByDesc(Brand::getHotScore)));
        PageResult<RobotSummaryVO> products = robots(id, 1, 20);
        vo.setProductList(products.getList());
        return vo;
    }

    @Override
    public PageResult<RobotSummaryVO> robots(Long companyId, Integer pageNum, Integer pageSize) {
        List<Brand> brands = brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getCompanyId, companyId));
        if (brands.isEmpty()) {
            return PageResult.of(1, pageSize == null ? 20 : pageSize, 0, new ArrayList<>());
        }
        List<Long> brandIds = brands.stream().map(Brand::getId).collect(Collectors.toList());
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Robot> page = new Page<>(pn, ps);
        IPage<Robot> result = robotMapper.selectPage(page, Wrappers.<Robot>lambdaQuery()
                .in(Robot::getBrandId, brandIds)
                .orderByDesc(Robot::getHotScore));
        List<RobotSummaryVO> vos = result.getRecords().stream().map(r -> {
            RobotSummaryVO vo = new RobotSummaryVO();
            vo.setId(r.getId());
            vo.setName(r.getName());
            vo.setModel(r.getModel());
            vo.setCoverImage(r.getCoverImage());
            vo.setGuidePrice(r.getGuidePrice());
            vo.setStatus(r.getStatus());
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public List<String> regions() {
        List<Company> list = list(Wrappers.<Company>lambdaQuery().eq(Company::getStatus, 1));
        Set<String> set = new LinkedHashSet<>();
        for (Company c : list) {
            if (StrUtil.isNotBlank(c.getRegion())) {
                set.add(c.getRegion());
            }
        }
        return new ArrayList<>(set);
    }

    @Override
    public List<CompanyListVO> hot(int limit) {
        List<Company> list = list(Wrappers.<Company>lambdaQuery()
                .eq(Company::getStatus, 1)
                .orderByDesc(Company::getHotScore)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        return list.stream().map(this::toListVO).collect(Collectors.toList());
    }

    private CompanyListVO toListVO(Company c) {
        CompanyListVO vo = new CompanyListVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setLogo(c.getLogo());
        vo.setIntro(c.getIntro());
        vo.setFoundYear(c.getFoundYear());
        vo.setRegion(c.getRegion());
        vo.setWebsite(c.getWebsite());
        vo.setContactPhone(c.getContactPhone());
        vo.setContactEmail(c.getContactEmail());
        vo.setAddress(c.getAddress());
        vo.setTags(JsonUtils.parseStringList(c.getTags()));
        vo.setBrandCount(c.getBrandCount());
        vo.setProductCount(c.getProductCount());
        vo.setHotScore(c.getHotScore());
        return vo;
    }
}
