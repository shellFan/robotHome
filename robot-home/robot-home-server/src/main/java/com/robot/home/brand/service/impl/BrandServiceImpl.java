package com.robot.home.brand.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.brand.service.BrandService;
import com.robot.home.brand.vo.BrandDetailVO;
import com.robot.home.brand.vo.BrandLetterGroupVO;
import com.robot.home.brand.vo.BrandListVO;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.vo.RobotSummaryVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 品牌服务实现
 */
@Service
public class BrandServiceImpl extends ServiceImpl<BrandMapper, Brand> implements BrandService {

    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private RobotMapper robotMapper;

    @Override
    public PageResult<BrandListVO> page(String keyword, String initial, Boolean hot, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Brand> page = new Page<>(pn, ps);
        IPage<Brand> result = page(page, Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .eq(StrUtil.isNotBlank(initial), Brand::getInitial, initial)
                .like(StrUtil.isNotBlank(keyword), Brand::getName, keyword)
                .orderBy(Boolean.TRUE.equals(hot), false, Brand::getHotScore)
                .orderBy(true, true, Brand::getSort)
                .orderByDesc(Brand::getHotScore));
        List<BrandListVO> vos = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public BrandDetailVO detail(Long id) {
        Brand brand = getById(id);
        if (brand == null) {
            throw new BusinessException("品牌不存在");
        }
        BrandDetailVO vo = new BrandDetailVO();
        vo.setBrand(brand);
        if (brand.getCompanyId() != null) {
            Company company = companyMapper.selectById(brand.getCompanyId());
            if (company != null) {
                vo.setCompanyName(company.getName());
            }
        }
        PageResult<RobotSummaryVO> products = robots(id, 1, 12);
        vo.setProductCount(products.getTotal());
        vo.setProducts(products.getList());
        return vo;
    }

    @Override
    public PageResult<RobotSummaryVO> robots(Long brandId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Robot> page = new Page<>(pn, ps);
        IPage<Robot> result = robotMapper.selectPage(page, Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getBrandId, brandId)
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
    public List<BrandLetterGroupVO> groupByLetter() {
        List<Brand> brands = list(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByAsc(Brand::getInitial)
                .orderByDesc(Brand::getHotScore));
        Map<String, List<BrandListVO>> map = new LinkedHashMap<>();
        for (Brand b : brands) {
            String letter = StrUtil.isBlank(b.getInitial()) ? "#" : b.getInitial().toUpperCase();
            map.computeIfAbsent(letter, k -> new ArrayList<>()).add(toListVO(b));
        }
        return map.entrySet().stream().map(e -> {
            BrandLetterGroupVO g = new BrandLetterGroupVO();
            g.setLetter(e.getKey());
            g.setBrands(e.getValue());
            return g;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BrandListVO> hot(int limit) {
        List<Brand> brands = list(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByDesc(Brand::getHotScore)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        return brands.stream().map(this::toListVO).collect(Collectors.toList());
    }

    private BrandListVO toListVO(Brand b) {
        BrandListVO vo = new BrandListVO();
        vo.setId(b.getId());
        vo.setName(b.getName());
        vo.setLogo(b.getLogo());
        vo.setInitial(b.getInitial());
        vo.setIntro(b.getIntro());
        vo.setCountry(b.getCountry());
        vo.setFoundYear(b.getFoundYear());
        vo.setWebsite(b.getWebsite());
        vo.setCompanyId(b.getCompanyId());
        vo.setRobotCount(b.getRobotCount());
        vo.setHotScore(b.getHotScore());
        if (b.getCompanyId() != null) {
            Company c = companyMapper.selectById(b.getCompanyId());
            if (c != null) {
                vo.setCompanyName(c.getName());
            }
        }
        return vo;
    }
}
