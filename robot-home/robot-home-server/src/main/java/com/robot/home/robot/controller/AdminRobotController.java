package com.robot.home.robot.controller;

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
import com.robot.home.common.util.XssUtils;
import com.robot.home.robot.dto.ParamValueSaveDTO;
import com.robot.home.robot.dto.RobotCategoryDTO;
import com.robot.home.robot.dto.RobotDTO;
import com.robot.home.robot.dto.RobotParamDefDTO;
import com.robot.home.robot.dto.RobotParamGroupDTO;
import com.robot.home.robot.dto.RobotParamTemplateDTO;
import com.robot.home.robot.dto.RobotSeriesDTO;
import com.robot.home.robot.entity.Robot;

import javax.validation.Valid;
import com.robot.home.robot.entity.RobotCategory;
import com.robot.home.robot.entity.RobotImage;
import com.robot.home.robot.entity.RobotParamDef;
import com.robot.home.robot.entity.RobotParamGroup;
import com.robot.home.robot.entity.RobotParamTemplate;
import com.robot.home.robot.entity.RobotParamValue;
import com.robot.home.robot.entity.RobotPrice;
import com.robot.home.robot.entity.RobotSeries;
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
import com.robot.home.robot.mapper.RobotSeriesMapper;
import com.robot.home.robot.mapper.RobotTagMapper;
import com.robot.home.robot.mapper.RobotVideoMapper;
import com.robot.home.robot.vo.ParamTemplateDetailVO;
import com.robot.home.security.RequirePermission;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 后台机器人管理：分类 / 系列 / 型号 / 参数模板 / 参数值 / 图片 / 视频 / 价格 / 标签
 */
@RestController
@RequestMapping("/api/admin/robots")
public class AdminRobotController {

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotCategoryMapper categoryMapper;
    @Resource
    private RobotSeriesMapper seriesMapper;
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
    private RedisUtils redisUtils;

    /** 清空机器人相关缓存（筛选器+分类树） */
    private void clearRobotCache() {
        for (String key : redisUtils.keys(Constants.CACHE_FILTER_PREFIX + "*")) {
            redisUtils.delete(key);
        }
        for (String key : redisUtils.keys(Constants.CACHE_CATEGORY_PREFIX + "*")) {
            redisUtils.delete(key);
        }
    }

    // ---------------- 型号 ----------------

    @GetMapping
    @RequirePermission("robot:list")
    public Result<PageResult<Robot>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Long brandId,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Robot> page = new Page<>(pn, ps);
        IPage<Robot> result = robotMapper.selectPage(page, Wrappers.<Robot>lambdaQuery()
                .likeRight(StrUtil.isNotBlank(keyword), Robot::getName, keyword)
                .eq(categoryId != null, Robot::getCategoryId, categoryId)
                .eq(brandId != null, Robot::getBrandId, brandId)
                .eq(status != null, Robot::getStatus, status)
                .orderByDesc(Robot::getId));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    @RequirePermission("robot:list")
    public Result<Robot> detail(@PathVariable Long id) {
        Robot robot = robotMapper.selectById(id);
        if (robot == null) {
            throw new BusinessException("机器人不存在");
        }
        return Result.success(robot);
    }

    @PostMapping
    @RequirePermission("robot:add")
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> save(@RequestBody @Valid RobotDTO dto) {
        if (StrUtil.isBlank(dto.getName())) {
            throw new BusinessException("产品名称不能为空");
        }
        Robot robot = new Robot();
        BeanUtils.copyProperties(dto, robot, "imagesList", "videos", "prices");
        // HTML富文本白名单清洗
        if (robot.getDetail() != null) {
            robot.setDetail(XssUtils.clean(robot.getDetail()));
        }
        if (robot.getId() == null) {
            robot.setViewCount(0);
            robot.setFavoriteCount(0);
            robot.setCompareCount(0);
            robot.setInquiryCount(0);
            robot.setCommentCount(0);
            if (robot.getStatus() == null) {
                robot.setStatus(1);
            }
            robotMapper.insert(robot);
        } else {
            Robot exist = robotMapper.selectById(robot.getId());
            if (exist == null) {
                throw new BusinessException("机器人不存在");
            }
            robotMapper.updateById(robot);
        }
        // 附带保存图片 / 视频 / 价格
        if (dto.getImagesList() != null && !dto.getImagesList().isEmpty()) {
            imageMapper.delete(Wrappers.<RobotImage>lambdaQuery().eq(RobotImage::getRobotId, robot.getId()));
            int sort = 1;
            for (RobotImage img : dto.getImagesList()) {
                img.setId(null);
                img.setRobotId(robot.getId());
                img.setSort(sort++);
                imageMapper.insert(img);
            }
        }
        if (dto.getVideos() != null && !dto.getVideos().isEmpty()) {
            robotVideoMapper.delete(Wrappers.<RobotVideo>lambdaQuery().eq(RobotVideo::getRobotId, robot.getId()));
            int sort = 1;
            for (RobotVideo v : dto.getVideos()) {
                v.setId(null);
                v.setRobotId(robot.getId());
                v.setSort(sort++);
                robotVideoMapper.insert(v);
            }
        }
        if (dto.getPrices() != null && !dto.getPrices().isEmpty()) {
            priceMapper.delete(Wrappers.<RobotPrice>lambdaQuery().eq(RobotPrice::getRobotId, robot.getId()));
            for (RobotPrice p : dto.getPrices()) {
                p.setId(null);
                p.setRobotId(robot.getId());
                p.setUpdateTime(LocalDateTime.now());
                priceMapper.insert(p);
            }
        }
        clearRobotCache();
        return Result.success(robot.getId());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("robot:delete")
    public Result<Void> delete(@PathVariable Long id) {
        robotMapper.deleteById(id);
        clearRobotCache();
        return Result.success();
    }

    @PostMapping("/{id}/status")
    @RequirePermission("robot:edit")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Robot robot = new Robot();
        robot.setId(id);
        robot.setStatus(status);
        robotMapper.updateById(robot);
        clearRobotCache();
        return Result.success();
    }

    // ---------------- 分类 ----------------

    @GetMapping("/categories")
    @RequirePermission("robot:category")
    public Result<List<RobotCategory>> categories() {
        return Result.success(categoryMapper.selectList(Wrappers.<RobotCategory>lambdaQuery()
                .orderByAsc(RobotCategory::getSort)));
    }

    @PostMapping("/categories")
    @RequirePermission("robot:category")
    public Result<Long> saveCategory(@RequestBody @Valid RobotCategoryDTO dto) {
        if (StrUtil.isBlank(dto.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        RobotCategory entity = new RobotCategory();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getParentId() == null) {
            entity.setParentId(0L);
        }
        if (entity.getLevel() == null) {
            entity.setLevel(entity.getParentId() == 0L ? 1 : 2);
        }
        if (entity.getId() == null) {
            categoryMapper.insert(entity);
        } else {
            categoryMapper.updateById(entity);
        }
        clearRobotCache();
        return Result.success(entity.getId());
    }

    @DeleteMapping("/categories/{id}")
    @RequirePermission("robot:category")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        long childCount = categoryMapper.selectCount(Wrappers.<RobotCategory>lambdaQuery()
                .eq(RobotCategory::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("存在子分类，不能删除");
        }
        long robotCount = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery().eq(Robot::getCategoryId, id));
        if (robotCount > 0) {
            throw new BusinessException("该分类下仍有 " + robotCount + " 台机器人，不能删除");
        }
        categoryMapper.deleteById(id);
        clearRobotCache();
        return Result.success();
    }

    // ---------------- 系列 ----------------

    @GetMapping("/series")
    @RequirePermission("robot:series")
    public Result<List<RobotSeries>> series(@RequestParam(required = false) Long brandId) {
        return Result.success(seriesMapper.selectList(Wrappers.<RobotSeries>lambdaQuery()
                .eq(brandId != null, RobotSeries::getBrandId, brandId)
                .orderByAsc(RobotSeries::getSort)));
    }

    @PostMapping("/series")
    @RequirePermission("robot:series")
    public Result<Long> saveSeries(@RequestBody @Valid RobotSeriesDTO dto) {
        if (StrUtil.isBlank(dto.getName())) {
            throw new BusinessException("系列名称不能为空");
        }
        RobotSeries entity = new RobotSeries();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getId() == null) {
            seriesMapper.insert(entity);
        } else {
            seriesMapper.updateById(entity);
        }
        return Result.success(entity.getId());
    }

    @DeleteMapping("/series/{id}")
    @RequirePermission("robot:series")
    public Result<Void> deleteSeries(@PathVariable Long id) {
        long robotCount = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery().eq(Robot::getSeriesId, id));
        if (robotCount > 0) {
            throw new BusinessException("该系列下仍有 " + robotCount + " 台机器人，不能删除");
        }
        seriesMapper.deleteById(id);
        return Result.success();
    }

    // ---------------- 参数模板 ----------------

    @GetMapping("/templates")
    @RequirePermission("robot:template")
    public Result<List<RobotParamTemplate>> templates() {
        return Result.success(templateMapper.selectList(Wrappers.<RobotParamTemplate>lambdaQuery()
                .orderByAsc(RobotParamTemplate::getSort)));
    }

    @GetMapping("/templates/{id}")
    @RequirePermission("robot:template")
    public Result<ParamTemplateDetailVO> templateDetail(@PathVariable Long id) {
        RobotParamTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("参数模板不存在");
        }
        List<RobotParamGroup> groups = groupMapper.selectList(Wrappers.<RobotParamGroup>lambdaQuery()
                .eq(RobotParamGroup::getTemplateId, id)
                .orderByAsc(RobotParamGroup::getSort));
        ParamTemplateDetailVO vo = new ParamTemplateDetailVO();
        vo.setTemplate(template);
        List<com.robot.home.robot.vo.RobotParamGroupVO> groupVos = new ArrayList<>();
        for (RobotParamGroup g : groups) {
            List<RobotParamDef> defs = defMapper.selectList(Wrappers.<RobotParamDef>lambdaQuery()
                    .eq(RobotParamDef::getGroupId, g.getId())
                    .orderByAsc(RobotParamDef::getSort));
            com.robot.home.robot.vo.RobotParamGroupVO gvo = new com.robot.home.robot.vo.RobotParamGroupVO();
            gvo.setGroup(g);
            List<com.robot.home.robot.vo.RobotParamDefVO> defVos = new ArrayList<>();
            for (RobotParamDef d : defs) {
                com.robot.home.robot.vo.RobotParamDefVO dvo = new com.robot.home.robot.vo.RobotParamDefVO();
                dvo.setDef(d);
                defVos.add(dvo);
            }
            gvo.setDefs(defVos);
            groupVos.add(gvo);
        }
        vo.setGroups(groupVos);
        return Result.success(vo);
    }

    @PostMapping("/templates")
    @RequirePermission("robot:template")
    public Result<Long> saveTemplate(@RequestBody @Valid RobotParamTemplateDTO dto) {
        if (StrUtil.isBlank(dto.getName()) || dto.getCategoryId() == null) {
            throw new BusinessException("模板名称与关联分类不能为空");
        }
        RobotParamTemplate entity = new RobotParamTemplate();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getId() == null) {
            templateMapper.insert(entity);
        } else {
            templateMapper.updateById(entity);
        }
        return Result.success(entity.getId());
    }

    @DeleteMapping("/templates/{id}")
    @RequirePermission("robot:template")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        List<RobotParamGroup> groups = groupMapper.selectList(Wrappers.<RobotParamGroup>lambdaQuery()
                .eq(RobotParamGroup::getTemplateId, id));
        for (RobotParamGroup g : groups) {
            defMapper.delete(Wrappers.<RobotParamDef>lambdaQuery().eq(RobotParamDef::getGroupId, g.getId()));
        }
        groupMapper.delete(Wrappers.<RobotParamGroup>lambdaQuery().eq(RobotParamGroup::getTemplateId, id));
        templateMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/param-groups")
    @RequirePermission("robot:template")
    public Result<Long> saveParamGroup(@RequestBody @Valid RobotParamGroupDTO dto) {
        if (StrUtil.isBlank(dto.getName()) || dto.getTemplateId() == null) {
            throw new BusinessException("分组名称与模板不能为空");
        }
        RobotParamGroup entity = new RobotParamGroup();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getId() == null) {
            groupMapper.insert(entity);
        } else {
            groupMapper.updateById(entity);
        }
        return Result.success(entity.getId());
    }

    @DeleteMapping("/param-groups/{id}")
    @RequirePermission("robot:template")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteParamGroup(@PathVariable Long id) {
        defMapper.delete(Wrappers.<RobotParamDef>lambdaQuery().eq(RobotParamDef::getGroupId, id));
        groupMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/param-defs")
    @RequirePermission("robot:template")
    public Result<Long> saveParamDef(@RequestBody @Valid RobotParamDefDTO dto) {
        if (StrUtil.isBlank(dto.getName()) || dto.getGroupId() == null) {
            throw new BusinessException("参数名与分组不能为空");
        }
        RobotParamDef entity = new RobotParamDef();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getIsShow() == null) {
            entity.setIsShow(1);
        }
        if (entity.getIsCompare() == null) {
            entity.setIsCompare(0);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getId() == null) {
            defMapper.insert(entity);
        } else {
            defMapper.updateById(entity);
        }
        return Result.success(entity.getId());
    }

    @DeleteMapping("/param-defs/{id}")
    @RequirePermission("robot:template")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteParamDef(@PathVariable Long id) {
        valueMapper.delete(Wrappers.<RobotParamValue>lambdaQuery().eq(RobotParamValue::getDefId, id));
        defMapper.deleteById(id);
        return Result.success();
    }

    // ---------------- 参数值 ----------------

    @GetMapping("/{id}/params")
    @RequirePermission("robot:edit")
    public Result<List<RobotParamValue>> params(@PathVariable Long id) {
        return Result.success(valueMapper.selectList(Wrappers.<RobotParamValue>lambdaQuery()
                .eq(RobotParamValue::getRobotId, id)));
    }

    @PostMapping("/params")
    @RequirePermission("robot:edit")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveParams(@RequestBody @Valid ParamValueSaveDTO dto) {
        if (dto.getRobotId() == null || dto.getItems() == null) {
            throw new BusinessException("参数不能为空");
        }
        for (ParamValueSaveDTO.Item item : dto.getItems()) {
            if (item.getDefId() == null) {
                continue;
            }
            RobotParamValue exist = valueMapper.selectOne(Wrappers.<RobotParamValue>lambdaQuery()
                    .eq(RobotParamValue::getRobotId, dto.getRobotId())
                    .eq(RobotParamValue::getDefId, item.getDefId())
                    .last("LIMIT 1"));
            if (StrUtil.isBlank(item.getValue())) {
                if (exist != null) {
                    valueMapper.deleteById(exist.getId());
                }
                continue;
            }
            if (exist != null) {
                exist.setValue(item.getValue());
                exist.setUpdateTime(LocalDateTime.now());
                valueMapper.updateById(exist);
            } else {
                RobotParamValue value = new RobotParamValue();
                value.setRobotId(dto.getRobotId());
                value.setDefId(item.getDefId());
                value.setValue(item.getValue());
                value.setUpdateTime(LocalDateTime.now());
                valueMapper.insert(value);
            }
        }
        return Result.success();
    }

    // ---------------- 图片 / 视频 / 价格 / 标签 ----------------

    @GetMapping("/{id}/images")
    @RequirePermission("robot:image")
    public Result<List<RobotImage>> images(@PathVariable Long id) {
        return Result.success(imageMapper.selectList(Wrappers.<RobotImage>lambdaQuery()
                .eq(RobotImage::getRobotId, id).orderByAsc(RobotImage::getSort)));
    }

    @PostMapping("/{id}/images")
    @RequirePermission("robot:image")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveImages(@PathVariable Long id, @RequestBody List<RobotImage> images) {
        imageMapper.delete(Wrappers.<RobotImage>lambdaQuery().eq(RobotImage::getRobotId, id));
        if (images == null) {
            return Result.success();
        }
        int sort = 1;
        for (RobotImage img : images) {
            img.setId(null);
            img.setRobotId(id);
            img.setSort(sort++);
            imageMapper.insert(img);
        }
        return Result.success();
    }

    @GetMapping("/{id}/videos")
    @RequirePermission("robot:video")
    public Result<List<RobotVideo>> videos(@PathVariable Long id) {
        return Result.success(robotVideoMapper.selectList(Wrappers.<RobotVideo>lambdaQuery()
                .eq(RobotVideo::getRobotId, id).orderByAsc(RobotVideo::getSort)));
    }

    @PostMapping("/{id}/videos")
    @RequirePermission("robot:video")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveVideos(@PathVariable Long id, @RequestBody List<RobotVideo> videos) {
        robotVideoMapper.delete(Wrappers.<RobotVideo>lambdaQuery().eq(RobotVideo::getRobotId, id));
        if (videos == null) {
            return Result.success();
        }
        int sort = 1;
        for (RobotVideo v : videos) {
            v.setId(null);
            v.setRobotId(id);
            v.setSort(sort++);
            robotVideoMapper.insert(v);
        }
        return Result.success();
    }

    @GetMapping("/{id}/prices")
    @RequirePermission("robot:edit")
    public Result<List<RobotPrice>> prices(@PathVariable Long id) {
        return Result.success(priceMapper.selectList(Wrappers.<RobotPrice>lambdaQuery()
                .eq(RobotPrice::getRobotId, id)));
    }

    @PostMapping("/{id}/prices")
    @RequirePermission("robot:edit")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> savePrices(@PathVariable Long id, @RequestBody List<RobotPrice> prices) {
        priceMapper.delete(Wrappers.<RobotPrice>lambdaQuery().eq(RobotPrice::getRobotId, id));
        if (prices == null) {
            return Result.success();
        }
        for (RobotPrice p : prices) {
            p.setId(null);
            p.setRobotId(id);
            p.setUpdateTime(LocalDateTime.now());
            priceMapper.insert(p);
        }
        return Result.success();
    }

    @GetMapping("/{id}/tags")
    @RequirePermission("robot:edit")
    public Result<List<RobotTag>> tags(@PathVariable Long id) {
        return Result.success(tagMapper.selectList(Wrappers.<RobotTag>lambdaQuery()
                .eq(RobotTag::getRobotId, id)));
    }

    /**
     * 保存标签：按 tagType 整组覆盖
     */
    @PostMapping("/{id}/tags")
    @RequirePermission("robot:edit")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveTags(@PathVariable Long id,
                                 @RequestParam String tagType,
                                 @RequestBody List<String> values) {
        tagMapper.delete(Wrappers.<RobotTag>lambdaQuery()
                .eq(RobotTag::getRobotId, id)
                .eq(RobotTag::getTagType, tagType));
        if (values == null) {
            return Result.success();
        }
        Set<String> distinct = new HashSet<>(values);
        for (String v : distinct) {
            if (StrUtil.isBlank(v)) {
                continue;
            }
            RobotTag tag = new RobotTag();
            tag.setRobotId(id);
            tag.setTagType(tagType);
            tag.setTagValue(v.trim());
            tagMapper.insert(tag);
        }
        return Result.success();
    }
}
