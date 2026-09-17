package com.robot.home.search.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.search.entity.SearchAlias;
import com.robot.home.search.entity.SearchZeroResult;
import com.robot.home.search.service.SearchAliasService;
import com.robot.home.search.service.SearchZeroResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * 搜索管理API — 路径 /api/admin/search/** 由AdminInterceptor保护
 */
@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
public class SearchAdminController {

    private final SearchAliasService aliasService;
    private final SearchZeroResultService zeroResultService;

    // ---- Alias 管理 ----

    @GetMapping("/aliases")
    public Result<PageResult<SearchAlias>> listAliases(
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(aliasService.adminList(targetType, pageNum, pageSize));
    }

    @PostMapping("/aliases")
    public Result<Map<String, Object>> createAlias(@RequestBody SearchAlias alias) {
        Long id = aliasService.create(alias);
        return Result.success(Collections.singletonMap("id", id));
    }

    @PutMapping("/aliases/{id}")
    public Result<Void> updateAlias(@PathVariable Long id, @RequestBody SearchAlias alias) {
        aliasService.update(id, alias);
        return Result.success();
    }

    @DeleteMapping("/aliases/{id}")
    public Result<Void> deleteAlias(@PathVariable Long id) {
        // SearchAliasService没有delete方法，用toggleStatus代替
        aliasService.toggleStatus(id);
        return Result.success();
    }

    // ---- ZeroResult 管理 ----

    @GetMapping("/zero-results")
    public Result<PageResult<SearchZeroResult>> listZeroResults(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(zeroResultService.adminList(pageNum, pageSize));
    }

    @PutMapping("/zero-results/{id}/action")
    public Result<Void> updateZeroResultAction(@PathVariable Long id, @RequestParam String action) {
        zeroResultService.updateAction(id, action);
        return Result.success();
    }
}