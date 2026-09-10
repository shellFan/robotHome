package com.robot.home.search.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.search.entity.SearchSuggestion;
import com.robot.home.search.mapper.SearchSuggestionMapper;
import com.robot.home.search.service.SearchSuggestionService;
import com.robot.home.security.RequirePermission;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 后台搜索建议管理：分页查询 / 新增 / 更新 / 删除
 */
@RestController
@RequestMapping("/api/admin/search-suggestions")
public class AdminSearchSuggestionController {

    @Resource
    private SearchSuggestionMapper searchSuggestionMapper;

    @Resource
    private SearchSuggestionService searchSuggestionService;

    /**
     * 分页查询搜索建议
     */
    @GetMapping
    @RequirePermission("search:list")
    public Result<PageResult<SearchSuggestion>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<SearchSuggestion> page = new Page<>(pn, ps);
        IPage<SearchSuggestion> result = searchSuggestionMapper.selectPage(page,
                Wrappers.<SearchSuggestion>lambdaQuery()
                        .like(StrUtil.isNotBlank(keyword), SearchSuggestion::getKeyword, keyword)
                        .eq(StrUtil.isNotBlank(type), SearchSuggestion::getType, type)
                        .eq(enabled != null, SearchSuggestion::getEnabled, enabled)
                        .orderByDesc(SearchSuggestion::getWeight));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    /**
     * 查看搜索建议详情
     */
    @GetMapping("/{id}")
    @RequirePermission("search:list")
    public Result<SearchSuggestion> detail(@PathVariable Long id) {
        SearchSuggestion suggestion = searchSuggestionMapper.selectById(id);
        if (suggestion == null) {
            throw new BusinessException("搜索建议不存在");
        }
        return Result.success(suggestion);
    }

    /**
     * 新增搜索建议
     */
    @PostMapping
    @RequirePermission("search:edit")
    public Result<Long> add(@RequestBody SearchSuggestion suggestion) {
        if (StrUtil.isBlank(suggestion.getKeyword())) {
            throw new BusinessException("关键词不能为空");
        }
        suggestion.setId(null);
        if (suggestion.getWeight() == null) {
            suggestion.setWeight(1);
        }
        if (suggestion.getEnabled() == null) {
            suggestion.setEnabled(1);
        }
        return Result.success(searchSuggestionService.add(suggestion));
    }

    /**
     * 更新搜索建议
     */
    @PutMapping("/{id}")
    @RequirePermission("search:edit")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody SearchSuggestion suggestion) {
        SearchSuggestion existing = searchSuggestionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("搜索建议不存在");
        }
        suggestion.setId(id);
        searchSuggestionService.update(suggestion);
        return Result.success();
    }

    /**
     * 删除搜索建议
     */
    @DeleteMapping("/{id}")
    @RequirePermission("search:edit")
    public Result<Void> delete(@PathVariable Long id) {
        searchSuggestionService.delete(id);
        return Result.success();
    }

    /**
     * 批量启用/禁用搜索建议
     */
    @PostMapping("/toggle-enabled")
    @RequirePermission("search:edit")
    public Result<Void> toggleEnabled(@RequestParam Long id, @RequestParam Integer enabled) {
        SearchSuggestion existing = searchSuggestionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("搜索建议不存在");
        }
        SearchSuggestion update = new SearchSuggestion();
        update.setId(id);
        update.setEnabled(enabled != null && enabled == 1 ? 1 : 0);
        searchSuggestionMapper.updateById(update);
        return Result.success();
    }
}