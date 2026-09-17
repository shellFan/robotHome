package com.robot.home.company.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.JsonUtils;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.service.CompanyService;
import com.robot.home.company.vo.CompanyDetailVO;
import com.robot.home.company.vo.CompanyListVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.vo.RobotSummaryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 企业服务实现
 */
@Service
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, Company> implements CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyServiceImpl.class);

    private static final long CACHE_SECONDS = 300L;

    @Resource
    private BrandMapper brandMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public PageResult<CompanyListVO> page(String keyword, String region, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Company> page = new Page<>(pn, ps);
        IPage<Company> result = page(page, Wrappers.<Company>lambdaQuery()
                .eq(Company::getStatus, 1)
                .eq(StrUtil.isNotBlank(region), Company::getRegion, region)
                .and(StrUtil.isNotBlank(keyword), w -> w.likeRight(Company::getName, keyword).or().like(Company::getIntro, keyword))
                .orderByDesc(Company::getHotScore));
        List<CompanyListVO> vos = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    @Override
    public CompanyDetailVO detail(Long id) {
        String key = Constants.CACHE_COMPANY_PREFIX + "detail:" + id;
        try {
            String cached = redisUtils.get(key);
            if (cached != null) {
                try {
                    CompanyDetailVO vo = JSONUtil.toBean(cached, CompanyDetailVO.class);
                    if (vo != null) {
                        return vo;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败时回源数据库
                }
            }
        } catch (Exception e) {
            log.warn("Redis企业详情缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }
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
        try {
            redisUtils.set(key, JSONUtil.toJsonStr(vo), CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis企业详情缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
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
        String key = Constants.CACHE_COMPANY_PREFIX + "regions";
        try {
            String cached = redisUtils.get(key);
            if (cached != null) {
                try {
                    List<String> list = JSONUtil.toList(JSONUtil.parseArray(cached), String.class);
                    if (list != null) {
                        return list;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败时回源数据库
                }
            }
        } catch (Exception e) {
            log.warn("Redis企业地区缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }
        List<Company> list = list(Wrappers.<Company>lambdaQuery().eq(Company::getStatus, 1));
        Set<String> set = new LinkedHashSet<>();
        for (Company c : list) {
            if (StrUtil.isNotBlank(c.getRegion())) {
                set.add(c.getRegion());
            }
        }
        List<String> result = new ArrayList<>(set);
        try {
            redisUtils.set(key, JSONUtil.toJsonStr(result), CACHE_SECONDS * 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis企业地区缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
        return result;
    }

    @Override
    public List<CompanyListVO> hot(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        String key = Constants.CACHE_COMPANY_PREFIX + "hot:" + size;
        try {
            String cached = redisUtils.get(key);
            if (cached != null) {
                try {
                    List<CompanyListVO> list = JSONUtil.toList(JSONUtil.parseArray(cached), CompanyListVO.class);
                    if (list != null) {
                        return list;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败时回源数据库
                }
            }
        } catch (Exception e) {
            log.warn("Redis热门企业缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }
        List<Company> list = list(Wrappers.<Company>lambdaQuery()
                .eq(Company::getStatus, 1)
                .orderByDesc(Company::getHotScore)
                .last("LIMIT " + size));
        List<CompanyListVO> result = list.stream().map(this::toListVO).collect(Collectors.toList());
        try {
            redisUtils.set(key, JSONUtil.toJsonStr(result), CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis热门企业缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
        return result;
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
