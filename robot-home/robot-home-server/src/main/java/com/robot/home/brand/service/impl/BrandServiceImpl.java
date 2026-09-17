package com.robot.home.brand.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
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
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.vo.RobotSummaryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 品牌服务实现
 */
@Service
public class BrandServiceImpl extends ServiceImpl<BrandMapper, Brand> implements BrandService {

    private static final Logger log = LoggerFactory.getLogger(BrandServiceImpl.class);

    private static final long CACHE_SECONDS = 600L;

    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public PageResult<BrandListVO> page(String keyword, String initial, Boolean hot, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Brand> page = new Page<>(pn, ps);
        IPage<Brand> result = page(page, Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .eq(StrUtil.isNotBlank(initial), Brand::getInitial, initial)
                .likeRight(StrUtil.isNotBlank(keyword), Brand::getName, keyword)
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
        String key = Constants.CACHE_BRAND_PREFIX + "letter";
        try {
            String cached = redisUtils.get(key);
            if (cached != null) {
                try {
                    List<BrandLetterGroupVO> list = JSONUtil.toList(JSONUtil.parseArray(cached), BrandLetterGroupVO.class);
                    if (list != null) {
                        return list;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败时回源数据库
                }
            }
        } catch (Exception e) {
            log.warn("Redis品牌字母分组缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }
        List<Brand> brands = list(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByAsc(Brand::getInitial)
                .orderByDesc(Brand::getHotScore));
        Map<String, List<BrandListVO>> map = new LinkedHashMap<>();
        for (Brand b : brands) {
            String letter = StrUtil.isBlank(b.getInitial()) ? "#" : b.getInitial().toUpperCase();
            map.computeIfAbsent(letter, k -> new ArrayList<>()).add(toListVO(b));
        }
        List<BrandLetterGroupVO> result = map.entrySet().stream().map(e -> {
            BrandLetterGroupVO g = new BrandLetterGroupVO();
            g.setLetter(e.getKey());
            g.setBrands(e.getValue());
            return g;
        }).collect(Collectors.toList());
        try {
            redisUtils.set(key, JSONUtil.toJsonStr(result), CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis品牌字母分组缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
        return result;
    }

    @Override
    public List<BrandListVO> hot(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        String key = Constants.CACHE_BRAND_PREFIX + "hot:" + size;
        try {
            String cached = redisUtils.get(key);
            if (cached != null) {
                try {
                    List<BrandListVO> list = JSONUtil.toList(JSONUtil.parseArray(cached), BrandListVO.class);
                    if (list != null) {
                        return list;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败时回源数据库
                }
            }
        } catch (Exception e) {
            log.warn("Redis热门品牌缓存读取失败，降级到DB查询: error={}", e.getMessage());
        }
        List<Brand> brands = list(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByDesc(Brand::getHotScore)
                .last("LIMIT " + size));
        List<BrandListVO> result = brands.stream().map(this::toListVO).collect(Collectors.toList());
        try {
            redisUtils.set(key, JSONUtil.toJsonStr(result), CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis热门品牌缓存写入失败（不影响返回）: error={}", e.getMessage());
        }
        return result;
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
