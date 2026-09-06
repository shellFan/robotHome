package com.robot.home.robot.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.service.BizCounter;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.favorite.service.FavoriteService;
import com.robot.home.history.service.HistoryService;
import com.robot.home.common.Constants;
import com.robot.home.robot.dto.RobotQuery;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotCategory;
import com.robot.home.robot.entity.RobotImage;
import com.robot.home.robot.entity.RobotParamDef;
import com.robot.home.robot.entity.RobotParamGroup;
import com.robot.home.robot.entity.RobotParamTemplate;
import com.robot.home.robot.entity.RobotParamValue;
import com.robot.home.robot.entity.RobotTag;
import com.robot.home.robot.entity.RobotVideo;
import com.robot.home.robot.mapper.RobotCategoryMapper;
import com.robot.home.robot.mapper.RobotImageMapper;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.mapper.RobotParamDefMapper;
import com.robot.home.robot.mapper.RobotParamGroupMapper;
import com.robot.home.robot.mapper.RobotParamTemplateMapper;
import com.robot.home.robot.mapper.RobotParamValueMapper;
import com.robot.home.robot.mapper.RobotPriceMapper;
import com.robot.home.robot.mapper.RobotTagMapper;
import com.robot.home.robot.mapper.RobotVideoMapper;
import com.robot.home.robot.service.RobotService;
import com.robot.home.robot.vo.BrandOptionVO;
import com.robot.home.robot.vo.CategoryNodeVO;
import com.robot.home.robot.vo.CompareGroupVO;
import com.robot.home.robot.vo.CompareRobotVO;
import com.robot.home.robot.vo.CompareRowVO;
import com.robot.home.robot.vo.CompareVO;
import com.robot.home.robot.vo.PriceRangeVO;
import com.robot.home.robot.vo.RelatedArticleVO;
import com.robot.home.robot.vo.RobotDetailVO;
import com.robot.home.robot.vo.RobotFilterVO;
import com.robot.home.robot.vo.RobotListVO;
import com.robot.home.robot.vo.RobotParamDefVO;
import com.robot.home.robot.vo.RobotParamGroupVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 机器人服务实现：库 / 详情 / 参数 / 对比 / 筛选
 */
@Service
public class RobotServiceImpl extends ServiceImpl<RobotMapper, Robot> implements RobotService {

    /** 对比最多支持的机器人数量 */
    private static final int MAX_COMPARE = 4;
    private static final long CACHE_SECONDS = 600L;

    @Resource
    private RobotCategoryMapper categoryMapper;
    @Resource
    private RobotImageMapper imageMapper;
    @Resource
    private RobotVideoMapper robotVideoMapper;
    @Resource
    private RobotPriceMapper priceMapper;
    @Resource
    private RobotParamTemplateMapper templateMapper;
    @Resource
    private RobotParamGroupMapper groupMapper;
    @Resource
    private RobotParamDefMapper defMapper;
    @Resource
    private RobotParamValueMapper valueMapper;
    @Resource
    private RobotTagMapper tagMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private FavoriteService favoriteService;
    @Resource
    private HistoryService historyService;
    @Resource
    private BizCounter bizCounter;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public PageResult<RobotListVO> page(RobotQuery query, Long currentUserId) {
        if (query == null) {
            query = new RobotQuery();
        }
        expandCategoryIds(query);
        int pn = PageUtils.normalizePageNum(query.getPageNum());
        int ps = PageUtils.normalizePageSize(query.getPageSize());
        Page<RobotListVO> page = new Page<>(pn, ps);
        IPage<RobotListVO> result = baseMapper.selectRobotPage(page, query);
        enrich(result.getRecords(), currentUserId);
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public RobotDetailVO detail(Long id, Long currentUserId) {
        Robot robot = getById(id);
        if (robot == null) {
            throw new BusinessException("机器人不存在");
        }
        RobotDetailVO vo = new RobotDetailVO();
        vo.setRobot(robot);

        // 批量预加载关联数据，避免N+1查询
        Set<Long> brandIds = new HashSet<>();
        Set<Long> companyIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        if (robot.getBrandId() != null) {
            brandIds.add(robot.getBrandId());
        }
        if (robot.getCategoryId() != null) {
            categoryIds.add(robot.getCategoryId());
        }

        // 批量查询品牌
        Map<Long, Brand> brandMap = Collections.emptyMap();
        if (!brandIds.isEmpty()) {
            brandMap = brandMapper.selectBatchIds(brandIds).stream()
                    .collect(Collectors.toMap(Brand::getId, b -> b));
        }

        // 批量查询分类
        Map<Long, RobotCategory> categoryMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            categoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(RobotCategory::getId, c -> c));
        }

        // 填充品牌和公司信息
        if (robot.getBrandId() != null) {
            Brand brand = brandMap.get(robot.getBrandId());
            if (brand != null) {
                vo.setBrandId(brand.getId());
                vo.setBrandName(brand.getName());
                vo.setBrandLogo(brand.getLogo());
                if (brand.getCompanyId() != null) {
                    companyIds.add(brand.getCompanyId());
                }
            }
        }

        // 填充分类信息，并收集父分类ID
        Set<Long> parentCategoryIds = new HashSet<>();
        if (robot.getCategoryId() != null) {
            RobotCategory cat = categoryMap.get(robot.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
                if (cat.getParentId() != null && cat.getParentId() > 0) {
                    parentCategoryIds.add(cat.getParentId());
                }
            }
        }

        // 批量查询公司
        Map<Long, Company> companyMap = Collections.emptyMap();
        if (!companyIds.isEmpty()) {
            companyMap = companyMapper.selectBatchIds(companyIds).stream()
                    .collect(Collectors.toMap(Company::getId, c -> c));
        }

        // 批量查询父分类
        if (!parentCategoryIds.isEmpty()) {
            // 合并已有分类Map
            List<RobotCategory> parents = categoryMapper.selectBatchIds(parentCategoryIds);
            for (RobotCategory parent : parents) {
                categoryMap.put(parent.getId(), parent);
            }
        }

        // 填充公司信息
        if (robot.getBrandId() != null) {
            Brand brand = brandMap.get(robot.getBrandId());
            if (brand != null && brand.getCompanyId() != null) {
                Company company = companyMap.get(brand.getCompanyId());
                if (company != null) {
                    vo.setCompanyId(company.getId());
                    vo.setCompanyName(company.getName());
                }
            }
        }

        // 填充父分类名称
        if (robot.getCategoryId() != null) {
            RobotCategory cat = categoryMap.get(robot.getCategoryId());
            if (cat != null && cat.getParentId() != null && cat.getParentId() > 0) {
                RobotCategory parent = categoryMap.get(cat.getParentId());
                if (parent != null) {
                    vo.setParentCategoryName(parent.getName());
                }
            }
        }

        vo.setImages(images(id));
        vo.setVideos(videos(id));
        vo.setParamGroups(params(id));
        vo.setPrices(priceMapper.selectList(Wrappers.<com.robot.home.robot.entity.RobotPrice>lambdaQuery()
                .eq(com.robot.home.robot.entity.RobotPrice::getRobotId, id)));
        vo.setArticles(relatedArticles(id, 5));

        Map<String, List<String>> tags = tagsOf(id);
        vo.setScenes(tags.getOrDefault("scene", Collections.emptyList()));
        vo.setDevs(tags.getOrDefault("dev", Collections.emptyList()));
        vo.setAis(tags.getOrDefault("ai", Collections.emptyList()));

        if (currentUserId != null) {
            vo.setFavorited(favoriteService.check(currentUserId, "robot", id));
        } else {
            vo.setFavorited(false);
        }
        vo.setFavoriteCount(robot.getFavoriteCount() == null ? 0L : robot.getFavoriteCount().longValue());

        recordView(id, currentUserId);
        return vo;
    }

    @Override
    public List<RobotParamGroupVO> params(Long id) {
        Robot robot = getById(id);
        if (robot == null) {
            throw new BusinessException("机器人不存在");
        }
        RobotParamTemplate template = resolveTemplate(robot);
        if (template == null) {
            return Collections.emptyList();
        }
        Map<Long, String> valueMap = valueMapOf(id);
        List<RobotParamGroup> groups = groupMapper.selectList(Wrappers.<RobotParamGroup>lambdaQuery()
                .eq(RobotParamGroup::getTemplateId, template.getId())
                .orderByAsc(RobotParamGroup::getSort));

        // 批量查询所有组的参数定义，避免N+1查询
        List<Long> groupIds = groups.stream().map(RobotParamGroup::getId).collect(Collectors.toList());
        Map<Long, List<RobotParamDef>> defsByGroup = Collections.emptyMap();
        if (!groupIds.isEmpty()) {
            defsByGroup = defMapper.selectList(Wrappers.<RobotParamDef>lambdaQuery()
                    .in(RobotParamDef::getGroupId, groupIds)
                    .eq(RobotParamDef::getIsShow, 1)
                    .orderByAsc(RobotParamDef::getSort))
                    .stream()
                    .collect(Collectors.groupingBy(RobotParamDef::getGroupId));
        }

        List<RobotParamGroupVO> result = new ArrayList<>();
        for (RobotParamGroup group : groups) {
            List<RobotParamDef> defs = defsByGroup.getOrDefault(group.getId(), Collections.emptyList());
            List<RobotParamDefVO> defVos = new ArrayList<>();
            for (RobotParamDef def : defs) {
                RobotParamDefVO defVo = new RobotParamDefVO();
                defVo.setDef(def);
                defVo.setValue(valueMap.get(def.getId()));
                defVos.add(defVo);
            }
            RobotParamGroupVO gvo = new RobotParamGroupVO();
            gvo.setGroup(group);
            gvo.setDefs(defVos);
            result.add(gvo);
        }
        return result;
    }

    @Override
    public List<RobotImage> images(Long id) {
        return imageMapper.selectList(Wrappers.<RobotImage>lambdaQuery()
                .eq(RobotImage::getRobotId, id)
                .orderByAsc(RobotImage::getSort));
    }

    @Override
    public List<RobotVideo> videos(Long id) {
        return robotVideoMapper.selectList(Wrappers.<RobotVideo>lambdaQuery()
                .eq(RobotVideo::getRobotId, id)
                .orderByAsc(RobotVideo::getSort));
    }

    @Override
    public List<RelatedArticleVO> relatedArticles(Long id, int limit) {
        Robot robot = getById(id);
        if (robot == null) {
            return Collections.emptyList();
        }
        List<Article> articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .and(w -> w.eq(Article::getRobotId, id)
                        .or(robot.getBrandId() != null, q -> q.eq(Article::getBrandId, robot.getBrandId())))
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 20))));
        return articles.stream().map(a -> {
            RelatedArticleVO vo = new RelatedArticleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setCover(a.getCover());
            vo.setSummary(a.getSummary());
            vo.setPublishTime(a.getPublishTime());
            vo.setViewCount(a.getViewCount());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CompareVO compare(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要对比的机器人");
        }
        if (ids.size() > MAX_COMPARE) {
            throw new BusinessException("最多同时对比 " + MAX_COMPARE + " 台机器人");
        }
        List<RobotListVO> robots = sortByIds(baseMapper.selectListByIds(ids), ids);
        if (robots.isEmpty()) {
            throw new BusinessException("未找到要对比的机器人");
        }

        CompareVO vo = new CompareVO();
        List<CompareRobotVO> heads = robots.stream().map(r -> {
            CompareRobotVO h = new CompareRobotVO();
            h.setId(r.getId());
            h.setName(r.getName());
            h.setModel(r.getModel());
            h.setCoverImage(r.getCoverImage());
            h.setBrandName(r.getBrandName());
            h.setGuidePrice(r.getGuidePrice());
            return h;
        }).collect(Collectors.toList());
        vo.setRobots(heads);

        // 以第一台机器人的分类模板作为参数基准
        Robot first = getById(robots.get(0).getId());
        RobotParamTemplate template = resolveTemplate(first);
        List<CompareGroupVO> groups = new ArrayList<>();
        if (template != null) {
            List<RobotParamGroup> paramGroups = groupMapper.selectList(Wrappers.<RobotParamGroup>lambdaQuery()
                    .eq(RobotParamGroup::getTemplateId, template.getId())
                    .orderByAsc(RobotParamGroup::getSort));

            // 批量查询所有组的参数定义，避免N+1查询
            List<Long> groupIds = paramGroups.stream().map(RobotParamGroup::getId).collect(Collectors.toList());
            Map<Long, List<RobotParamDef>> defsByGroup = Collections.emptyMap();
            if (!groupIds.isEmpty()) {
                defsByGroup = defMapper.selectList(Wrappers.<RobotParamDef>lambdaQuery()
                        .in(RobotParamDef::getGroupId, groupIds)
                        .eq(RobotParamDef::getIsShow, 1)
                        .orderByAsc(RobotParamDef::getSort))
                        .stream()
                        .collect(Collectors.groupingBy(RobotParamDef::getGroupId));
            }

            // 批量查询各机器人的参数值，避免N+1查询
            List<Map<Long, String>> valueMaps = new ArrayList<>();
            List<Long> robotIds = robots.stream().map(RobotListVO::getId).collect(Collectors.toList());
            // 一次性查询所有机器人的参数值
            List<RobotParamValue> allValues = valueMapper.selectList(Wrappers.<RobotParamValue>lambdaQuery()
                    .in(RobotParamValue::getRobotId, robotIds));
            Map<Long, Map<Long, String>> valueMapByRobot = new HashMap<>();
            for (RobotParamValue v : allValues) {
                valueMapByRobot.computeIfAbsent(v.getRobotId(), k -> new HashMap<>())
                        .put(v.getDefId(), v.getValue());
            }
            for (RobotListVO r : robots) {
                valueMaps.add(valueMapByRobot.getOrDefault(r.getId(), Collections.emptyMap()));
            }

            for (RobotParamGroup group : paramGroups) {
                List<RobotParamDef> defs = defsByGroup.getOrDefault(group.getId(), Collections.emptyList());
                if (defs.isEmpty()) {
                    continue;
                }
                CompareGroupVO gvo = new CompareGroupVO();
                gvo.setGroupName(group.getName());
                List<CompareRowVO> rows = new ArrayList<>();
                for (RobotParamDef def : defs) {
                    CompareRowVO row = new CompareRowVO();
                    row.setDefId(def.getId());
                    row.setParamName(def.getName());
                    row.setUnit(def.getUnit());
                    List<String> values = new ArrayList<>();
                    Set<String> distinct = new LinkedHashSet<>();
                    for (Map<Long, String> vm : valueMaps) {
                        String v = vm.get(def.getId());
                        String display = StrUtil.isBlank(v) ? "-" : v;
                        values.add(display);
                        if (!"-".equals(display)) {
                            distinct.add(display);
                        }
                    }
                    row.setValues(values);
                    row.setDifferent(distinct.size() > 1);
                    rows.add(row);
                }
                gvo.setRows(rows);
                groups.add(gvo);
            }
        }
        vo.setGroups(groups);
        return vo;
    }

    /**
     * 按传入 id 顺序还原列表顺序（替代 MySQL 的 ORDER BY FIELD）
     */
    private List<RobotListVO> sortByIds(List<RobotListVO> list, List<Long> ids) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        Map<Long, RobotListVO> index = new HashMap<>();
        for (RobotListVO vo : list) {
            index.put(vo.getId(), vo);
        }
        List<RobotListVO> result = new ArrayList<>();
        for (Long id : ids) {
            RobotListVO vo = index.get(id);
            if (vo != null) {
                result.add(vo);
            }
        }
        // 兜底：补上未匹配到的记录，避免丢数据
        if (result.size() < list.size()) {
            for (RobotListVO vo : list) {
                if (!result.contains(vo)) {
                    result.add(vo);
                }
            }
        }
        return result;
    }

    @Override
    public RobotFilterVO filters() {
        String key = Constants.CACHE_FILTER_PREFIX + "robot";
        String cached = redisUtils.get(key);
        if (cached != null) {
            try {
                RobotFilterVO vo = JSONUtil.toBean(cached, RobotFilterVO.class);
                if (vo != null) {
                    return vo;
                }
            } catch (Exception ignored) {
                // 缓存解析失败时回源数据库
            }
        }
        RobotFilterVO vo = new RobotFilterVO();
        vo.setCategories(categoryTree());

        List<Brand> brands = brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByDesc(Brand::getHotScore)
                .last("LIMIT 50"));
        vo.setBrands(brands.stream().map(b -> {
            BrandOptionVO o = new BrandOptionVO();
            o.setId(b.getId());
            o.setName(b.getName());
            o.setLogo(b.getLogo());
            o.setInitial(b.getInitial());
            o.setRobotCount(b.getRobotCount());
            return o;
        }).collect(Collectors.toList()));

        vo.setScenes(distinctTagValues("scene"));
        vo.setDevs(distinctTagValues("dev"));
        vo.setAis(distinctTagValues("ai"));
        vo.setPriceRanges(Arrays.asList(
                new PriceRangeVO("1万以下", BigDecimal.ZERO, new BigDecimal("10000")),
                new PriceRangeVO("1-5万", new BigDecimal("10000"), new BigDecimal("50000")),
                new PriceRangeVO("5-20万", new BigDecimal("50000"), new BigDecimal("200000")),
                new PriceRangeVO("20-50万", new BigDecimal("200000"), new BigDecimal("500000")),
                new PriceRangeVO("50万以上", new BigDecimal("500000"), null)));
        redisUtils.set(key, JSONUtil.toJsonStr(vo), CACHE_SECONDS, TimeUnit.SECONDS);
        return vo;
    }

    @Override
    public List<RobotListVO> hot(int limit, Long currentUserId) {
        RobotQuery q = new RobotQuery();
        q.setSort("hot");
        q.setPageNum(1);
        q.setPageSize(Math.max(1, Math.min(limit, 50)));
        PageResult<RobotListVO> page = page(q, currentUserId);
        return page.getList();
    }

    @Override
    public List<RobotListVO> newest(int limit, Long currentUserId) {
        RobotQuery q = new RobotQuery();
        q.setSort("new");
        q.setPageNum(1);
        q.setPageSize(Math.max(1, Math.min(limit, 50)));
        PageResult<RobotListVO> page = page(q, currentUserId);
        return page.getList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordView(Long id, Long userId) {
        if (id == null) {
            return;
        }
        bizCounter.incr("robot", id, BizCounter.Field.VIEW);
        if (userId != null) {
            historyService.record(userId, "robot", id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordCompare(List<Long> ids) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                bizCounter.incr("robot", id, BizCounter.Field.COMPARE);
            }
        }
    }

    @Override
    public Robot getByIdOrNull(Long id) {
        return id == null ? null : getById(id);
    }

    // ---------------- 内部方法 ----------------

    /**
     * 一级分类展开为「自身 + 全部子分类」
     */
    private void expandCategoryIds(RobotQuery query) {
        Long categoryId = query.getCategoryId();
        if (categoryId == null) {
            return;
        }
        List<RobotCategory> children = categoryMapper.selectList(Wrappers.<RobotCategory>lambdaQuery()
                .eq(RobotCategory::getParentId, categoryId)
                .eq(RobotCategory::getStatus, 1));
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        for (RobotCategory c : children) {
            ids.add(c.getId());
        }
        query.setCategoryIds(ids);
    }

    /**
     * 解析机器人适用的参数模板：二级分类回退到一级分类模板
     */
    private RobotParamTemplate resolveTemplate(Robot robot) {
        if (robot == null || robot.getCategoryId() == null) {
            return null;
        }
        Long categoryId = robot.getCategoryId();
        RobotCategory cat = categoryMapper.selectById(categoryId);
        Long templateCategoryId = categoryId;
        if (cat != null && cat.getParentId() != null && cat.getParentId() > 0) {
            templateCategoryId = cat.getParentId();
        }
        RobotParamTemplate template = templateMapper.selectOne(Wrappers.<RobotParamTemplate>lambdaQuery()
                .eq(RobotParamTemplate::getCategoryId, templateCategoryId)
                .eq(RobotParamTemplate::getStatus, 1)
                .last("LIMIT 1"));
        if (template == null && !templateCategoryId.equals(categoryId)) {
            template = templateMapper.selectOne(Wrappers.<RobotParamTemplate>lambdaQuery()
                    .eq(RobotParamTemplate::getCategoryId, categoryId)
                    .eq(RobotParamTemplate::getStatus, 1)
                    .last("LIMIT 1"));
        }
        return template;
    }

    private Map<Long, String> valueMapOf(Long robotId) {
        List<RobotParamValue> values = valueMapper.selectList(Wrappers.<RobotParamValue>lambdaQuery()
                .eq(RobotParamValue::getRobotId, robotId));
        Map<Long, String> map = new HashMap<>();
        for (RobotParamValue v : values) {
            map.put(v.getDefId(), v.getValue());
        }
        return map;
    }

    private Map<String, List<String>> tagsOf(Long robotId) {
        List<RobotTag> tags = tagMapper.selectList(Wrappers.<RobotTag>lambdaQuery()
                .eq(RobotTag::getRobotId, robotId));
        Map<String, List<String>> map = new HashMap<>();
        for (RobotTag t : tags) {
            map.computeIfAbsent(t.getTagType(), k -> new ArrayList<>()).add(t.getTagValue());
        }
        return map;
    }

    /**
     * 为列表项补充收藏状态与标签
     */
    private void enrich(List<RobotListVO> list, Long currentUserId) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream().map(RobotListVO::getId).collect(Collectors.toList());
        Set<Long> favorited = currentUserId == null
                ? Collections.emptySet()
                : favoriteService.checkBatch(currentUserId, "robot", ids);

        List<RobotTag> tags = tagMapper.selectList(Wrappers.<RobotTag>lambdaQuery()
                .in(RobotTag::getRobotId, ids));
        Map<Long, List<String>> tagMap = new HashMap<>();
        for (RobotTag t : tags) {
            tagMap.computeIfAbsent(t.getRobotId(), k -> new ArrayList<>()).add(t.getTagValue());
        }

        for (RobotListVO vo : list) {
            vo.setFavorited(favorited.contains(vo.getId()));
            List<String> ts = tagMap.get(vo.getId());
            if (ts != null) {
                vo.setTags(new ArrayList<>(new LinkedHashSet<>(ts)));
            }
        }
    }

    private List<CategoryNodeVO> categoryTree() {
        String key = Constants.CACHE_CATEGORY_PREFIX + "robot";
        String cached = redisUtils.get(key);
        if (cached != null) {
            try {
                List<CategoryNodeVO> list = JSONUtil.toList(JSONUtil.parseArray(cached), CategoryNodeVO.class);
                if (list != null) {
                    return list;
                }
            } catch (Exception ignored) {
                // 缓存解析失败时回源数据库
            }
        }
        List<RobotCategory> all = categoryMapper.selectList(Wrappers.<RobotCategory>lambdaQuery()
                .eq(RobotCategory::getStatus, 1)
                .orderByAsc(RobotCategory::getSort));
        List<CategoryNodeVO> roots = new ArrayList<>();
        Map<Long, CategoryNodeVO> index = new HashMap<>();
        for (RobotCategory c : all) {
            CategoryNodeVO node = new CategoryNodeVO();
            node.setId(c.getId());
            node.setName(c.getName());
            node.setIcon(c.getIcon());
            node.setLevel(c.getLevel());
            node.setSort(c.getSort());
            node.setChildren(new ArrayList<>());
            index.put(c.getId(), node);
        }
        for (RobotCategory c : all) {
            CategoryNodeVO node = index.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(node);
            } else {
                CategoryNodeVO parent = index.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        redisUtils.set(key, JSONUtil.toJsonStr(roots), CACHE_SECONDS, TimeUnit.SECONDS);
        return roots;
    }

    private List<String> distinctTagValues(String tagType) {
        List<RobotTag> tags = tagMapper.selectList(Wrappers.<RobotTag>lambdaQuery()
                .eq(RobotTag::getTagType, tagType));
        Set<String> set = new LinkedHashSet<>();
        for (RobotTag t : tags) {
            if (StrUtil.isNotBlank(t.getTagValue())) {
                set.add(t.getTagValue());
            }
        }
        return new ArrayList<>(set);
    }
}
