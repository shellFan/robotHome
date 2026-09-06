package com.robot.home.tutorial.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.security.RequirePermission;
import com.robot.home.tutorial.entity.Tutorial;
import com.robot.home.tutorial.entity.TutorialCategory;
import com.robot.home.tutorial.mapper.TutorialCategoryMapper;
import com.robot.home.tutorial.mapper.TutorialMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台教程管理
 */
@RestController
@RequestMapping("/api/admin/tutorials")
public class AdminTutorialController {

    @Resource
    private TutorialMapper tutorialMapper;
    @Resource
    private TutorialCategoryMapper categoryMapper;

    @GetMapping
    @RequirePermission("tutorial:list")
    public Result<PageResult<Tutorial>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Tutorial> page = new Page<>(pn, ps);
        IPage<Tutorial> result = tutorialMapper.selectPage(page, Wrappers.<Tutorial>lambdaQuery()
                .likeRight(StrUtil.isNotBlank(keyword), Tutorial::getTitle, keyword)
                .eq(categoryId != null, Tutorial::getCategoryId, categoryId)
                .orderByDesc(Tutorial::getPublishTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    @RequirePermission("tutorial:list")
    public Result<Tutorial> detail(@PathVariable Long id) {
        Tutorial tutorial = tutorialMapper.selectById(id);
        if (tutorial == null) {
            throw new BusinessException("教程不存在");
        }
        return Result.success(tutorial);
    }

    @PostMapping
    @RequirePermission("tutorial:add")
    public Result<Long> save(@RequestBody Tutorial tutorial) {
        if (StrUtil.isBlank(tutorial.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        tutorial.setContent(XssUtils.clean(tutorial.getContent()));
        if (tutorial.getId() == null) {
            if (tutorial.getStatus() == null) {
                tutorial.setStatus(1);
            }
            if (tutorial.getPublishTime() == null) {
                tutorial.setPublishTime(LocalDateTime.now());
            }
            tutorialMapper.insert(tutorial);
        } else {
            if (tutorialMapper.selectById(tutorial.getId()) == null) {
                throw new BusinessException("教程不存在");
            }
            tutorialMapper.updateById(tutorial);
        }
        return Result.success(tutorial.getId());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("tutorial:delete")
    public Result<Void> delete(@PathVariable Long id) {
        tutorialMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/categories")
    @RequirePermission("tutorial:category")
    public Result<List<TutorialCategory>> categories() {
        return Result.success(categoryMapper.selectList(Wrappers.<TutorialCategory>lambdaQuery()
                .orderByAsc(TutorialCategory::getSort)));
    }

    @PostMapping("/categories")
    @RequirePermission("tutorial:category")
    public Result<Long> saveCategory(@RequestBody TutorialCategory category) {
        if (StrUtil.isBlank(category.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        if (category.getId() == null) {
            if (category.getStatus() == null) {
                category.setStatus(1);
            }
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return Result.success(category.getId());
    }

    @DeleteMapping("/categories/{id}")
    @RequirePermission("tutorial:category")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        long count = tutorialMapper.selectCount(Wrappers.<Tutorial>lambdaQuery().eq(Tutorial::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException("该分类下仍有 " + count + " 篇教程，不能删除");
        }
        categoryMapper.deleteById(id);
        return Result.success();
    }
}
