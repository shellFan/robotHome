package com.robot.home.selection.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotCategory;
import com.robot.home.robot.mapper.RobotCategoryMapper;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.selection.dto.SelectionSearchDTO;
import com.robot.home.selection.entity.RobotSelectionLog;
import com.robot.home.selection.mapper.RobotSelectionLogMapper;
import com.robot.home.selection.service.RobotSelectionService;
import com.robot.home.selection.vo.SelectionResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RobotSelectionServiceImpl implements RobotSelectionService {

    private static final Logger log = LoggerFactory.getLogger(RobotSelectionServiceImpl.class);

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotCategoryMapper categoryMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private RobotSelectionLogMapper selectionLogMapper;

    @Override
    public PageResult<SelectionResultVO> search(SelectionSearchDTO dto) {
        int pn = PageUtils.normalizePageNum(dto.getPageNum());
        int ps = PageUtils.normalizePageSize(dto.getPageSize());
        // 限制最大pageSize
        if (ps > 50) {
            ps = 50;
        }

        // Step 1: Resolve category name to ID for DB-level filtering
        Long categoryId = null;
        if (StrUtil.isNotBlank(dto.getCategory())) {
            RobotCategory cat = categoryMapper.selectOne(Wrappers.<RobotCategory>lambdaQuery()
                    .eq(RobotCategory::getName, dto.getCategory().trim())
                    .eq(RobotCategory::getStatus, 1)
                    .last("LIMIT 1"));
            if (cat != null) {
                categoryId = cat.getId();
            } else {
                // Category not found, no results
                logSelection(dto, 0);
                return PageResult.of(pn, ps, 0, new ArrayList<SelectionResultVO>());
            }
        }

        // Step 2: DB-level filtering by category, brand, price (push as much as possible to SQL)
        // Limit candidates to prevent unbounded memory growth (max 500)
        List<Robot> candidates = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getStatus, 1)
                .eq(categoryId != null, Robot::getCategoryId, categoryId)
                .in(dto.getBrandIds() != null && !dto.getBrandIds().isEmpty(), Robot::getBrandId, dto.getBrandIds())
                .ge(dto.getBudgetMin() != null, Robot::getGuidePrice, dto.getBudgetMin())
                .le(dto.getBudgetMax() != null, Robot::getGuidePrice, dto.getBudgetMax())
                .orderByDesc(Robot::getGuidePrice)
                .last("LIMIT 500"));

        if (candidates.isEmpty()) {
            logSelection(dto, 0);
            return PageResult.of(pn, ps, 0, new ArrayList<SelectionResultVO>());
        }

        // Step 2: Batch load brands and categories
        Set<Long> brandIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        for (Robot r : candidates) {
            if (r.getBrandId() != null) brandIds.add(r.getBrandId());
            if (r.getCategoryId() != null) categoryIds.add(r.getCategoryId());
        }
        Map<Long, String> brandNameMap = new HashMap<>();
        if (!brandIds.isEmpty()) {
            brandMapper.selectBatchIds(brandIds).forEach(b -> brandNameMap.put(b.getId(), b.getName()));
        }
        Map<Long, RobotCategory> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectBatchIds(categoryIds).forEach(c -> categoryMap.put(c.getId(), c));
        }

        // Step 3: Calculate match scores (category already filtered at SQL level)
        List<SelectionResultVO> results = new ArrayList<>();
        for (Robot r : candidates) {
            int score = 0;
            List<String> reasons = new ArrayList<>();
            List<SelectionResultVO.ParamItem> keyParams = new ArrayList<>();

            // Category match (30 points) — already filtered at SQL level, all candidates match
            if (categoryId != null && r.getCategoryId() != null) {
                score += 30;
                reasons.add("分类匹配");
            }

            // Budget match (25 points)
            if (dto.getBudgetMax() != null && r.getGuidePrice() != null
                    && r.getGuidePrice().compareTo(dto.getBudgetMax()) <= 0) {
                score += 25;
                reasons.add("预算内");
            }

            // Brand match (15 points)
            if (dto.getBrandIds() != null && !dto.getBrandIds().isEmpty()
                    && r.getBrandId() != null && dto.getBrandIds().contains(r.getBrandId())) {
                score += 15;
                reasons.add("品牌偏好");
            }

            // Usage/scene match (20 points) - check mainParams JSON or subtitle/description
            if (StrUtil.isNotBlank(dto.getUsage())) {
                String usage = dto.getUsage().toLowerCase();
                boolean usageMatch = false;
                // Check subtitle
                if (r.getSubtitle() != null && r.getSubtitle().toLowerCase().contains(usage)) {
                    usageMatch = true;
                }
                // Check mainParams JSON
                if (!usageMatch && r.getMainParams() != null) {
                    String mp = r.getMainParams().toLowerCase();
                    if (mp.contains(usage)) {
                        usageMatch = true;
                    }
                }
                if (usageMatch) {
                    score += 20;
                    reasons.add("场景匹配");
                }
            }

            // Dynamic filter match (10 points) - match against mainParams JSON fields
            if (dto.getFilters() != null && !dto.getFilters().isEmpty() && r.getMainParams() != null) {
                try {
                    String mpLower = r.getMainParams().toLowerCase();
                    int filterMatch = 0;
                    int filterTotal = dto.getFilters().size();
                    for (Map.Entry<String, String> entry : dto.getFilters().entrySet()) {
                        String val = entry.getValue();
                        if (val != null && mpLower.contains(val.toLowerCase())) {
                            filterMatch++;
                        }
                    }
                    if (filterTotal > 0 && filterMatch > 0) {
                        score += (int) (10.0 * filterMatch / filterTotal);
                        if (filterMatch == filterTotal) {
                            reasons.add("参数完全匹配");
                        }
                    }
                } catch (Exception e) {
                    log.debug("Filter matching error for robot {}: {}", r.getId(), e.getMessage());
                }
            }

            // Build VO
            SelectionResultVO vo = new SelectionResultVO();
            vo.setRobotId(r.getId());
            vo.setRobotName(r.getName());
            vo.setCoverImage(r.getCoverImage());
            vo.setGuidePrice(r.getGuidePrice());
            vo.setCategoryName(r.getCategoryId() != null && categoryMap.containsKey(r.getCategoryId())
                    ? categoryMap.get(r.getCategoryId()).getName() : null);
            vo.setBrandName(r.getBrandId() != null ? brandNameMap.get(r.getBrandId()) : null);
            vo.setMatchScore(score);
            vo.setMatchReason(reasons.isEmpty() ? "综合推荐" : String.join("、", reasons));

            // Key params from mainParams JSON (max 5)
            if (r.getMainParams() != null) {
                try {
                    cn.hutool.json.JSONArray arr = JSONUtil.parseArray(r.getMainParams());
                    for (int i = 0; i < arr.size() && keyParams.size() < 5; i++) {
                        cn.hutool.json.JSONObject obj = arr.getJSONObject(i);
                        SelectionResultVO.ParamItem item = new SelectionResultVO.ParamItem();
                        item.setLabel(obj.getStr("name"));
                        item.setValue(obj.getStr("value"));
                        keyParams.add(item);
                    }
                } catch (Exception e) {
                    log.debug("Parse mainParams error for robot {}: {}", r.getId(), e.getMessage());
                }
            }
            vo.setKeyParams(keyParams);

            results.add(vo);
        }

        // Step 5: Filter out 0-score results, then sort (stable: secondary sort by price DESC, then id DESC)
        results = results.stream().filter(r -> r.getMatchScore() > 0).collect(Collectors.toList());
        results.sort((a, b) -> {
            int scoreDiff = b.getMatchScore() - a.getMatchScore();
            if (scoreDiff != 0) return scoreDiff;
            // Secondary: higher price first (premium robots)
            int priceDiff = 0;
            if (a.getGuidePrice() != null && b.getGuidePrice() != null) {
                priceDiff = b.getGuidePrice().compareTo(a.getGuidePrice());
            } else if (a.getGuidePrice() != null) {
                priceDiff = -1;
            } else if (b.getGuidePrice() != null) {
                priceDiff = 1;
            }
            if (priceDiff != 0) return priceDiff;
            // Tertiary: higher ID first (newer robots)
            return Long.compare(b.getRobotId() != null ? b.getRobotId() : 0, a.getRobotId() != null ? a.getRobotId() : 0);
        });

        // Step 6: Paginate
        int total = results.size();
        int fromIndex = (pn - 1) * ps;
        int toIndex = Math.min(fromIndex + ps, total);
        List<SelectionResultVO> page;
        if (fromIndex >= total) {
            page = new ArrayList<>();
        } else {
            page = results.subList(fromIndex, toIndex);
        }

        logSelection(dto, total);
        return PageResult.of(pn, ps, total, page);
    }

    @Override
    public Map<String, Object> getFilters(String category) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Categories
        List<RobotCategory> categories = categoryMapper.selectList(
                Wrappers.<RobotCategory>lambdaQuery().eq(RobotCategory::getStatus, 1).orderByAsc(RobotCategory::getSort));
        List<Map<String, Object>> catList = new ArrayList<>();
        for (RobotCategory c : categories) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            catList.add(m);
        }
        result.put("categories", catList);

        // Brands
        List<Brand> brands = brandMapper.selectList(
                Wrappers.<Brand>lambdaQuery().eq(Brand::getStatus, 1).orderByAsc(Brand::getName));
        List<Map<String, Object>> brandList = new ArrayList<>();
        for (Brand b : brands) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            brandList.add(m);
        }
        result.put("brands", brandList);

        // Budget ranges
        List<Map<String, Object>> budgetRanges = new ArrayList<>();
        budgetRanges.add(range("0-5万", "0", "50000"));
        budgetRanges.add(range("5-20万", "50000", "200000"));
        budgetRanges.add(range("20-50万", "200000", "500000"));
        budgetRanges.add(range("50-100万", "500000", "1000000"));
        budgetRanges.add(range("100万以上", "1000000", null));
        result.put("budgetRanges", budgetRanges);

        // Usage scenes
        List<Map<String, Object>> usageScenes = new ArrayList<>();
        usageScenes.add(scene("工业制造", "INDUSTRIAL"));
        usageScenes.add(scene("物流仓储", "LOGISTICS"));
        usageScenes.add(scene("医疗健康", "MEDICAL"));
        usageScenes.add(scene("教育培训", "EDUCATION"));
        usageScenes.add(scene("服务行业", "SERVICE"));
        result.put("usageScenes", usageScenes);

        return result;
    }

    private Map<String, Object> range(String label, String min, String max) {
        Map<String, Object> m = new HashMap<>();
        m.put("label", label);
        m.put("min", min);
        m.put("max", max);
        return m;
    }

    private Map<String, Object> scene(String name, String code) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("code", code);
        return m;
    }

    @Override
    public void logSelection(SelectionSearchDTO dto, Integer resultCount) {
        try {
            RobotSelectionLog logEntity = new RobotSelectionLog();
            logEntity.setUserId(dto.getUserId());
            logEntity.setCategory(dto.getCategory());
            logEntity.setBudgetMin(dto.getBudgetMin());
            logEntity.setBudgetMax(dto.getBudgetMax());
            logEntity.setUsage(dto.getUsage());
            logEntity.setResultCount(resultCount);
            selectionLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.debug("Selection log insert failed: {}", e.getMessage());
        }
    }
}