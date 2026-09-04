package com.robot.home.search.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.search.service.SearchService;
import com.robot.home.search.vo.HotSearchVO;
import com.robot.home.search.vo.SearchItemVO;
import com.robot.home.search.vo.SearchResultVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 统一搜索
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Resource
    private SearchService searchService;

    /**
     * 综合搜索（按类型分组）
     */
    @GetMapping
    public Result<SearchResultVO> search(@RequestParam String keyword,
                                         @RequestParam(defaultValue = "5") Integer limit) {
        Long userId = SecurityUtils.currentUserId();
        SearchResultVO result = searchService.search(keyword, limit);
        searchService.record(keyword, userId);
        return Result.success(result);
    }

    /**
     * 指定类型分页搜索
     */
    @GetMapping("/{type}")
    public Result<PageResult<SearchItemVO>> searchByType(@PathVariable String type,
                                                         @RequestParam String keyword,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(searchService.searchByType(type, keyword, pageNum, pageSize));
    }

    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(searchService.suggest(keyword, limit));
    }

    @GetMapping("/hot")
    public Result<List<HotSearchVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(searchService.hotSearches(limit));
    }

    @GetMapping("/history")
    public Result<List<String>> history(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(searchService.history(userId, limit));
    }

    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        Long userId = SecurityUtils.requireUserId();
        searchService.clearHistory(userId);
        return Result.success();
    }
}
