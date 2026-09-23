package com.robot.home.search.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.search.service.SearchService;
import com.robot.home.search.service.SearchSuggestionService;
import com.robot.home.search.entity.SearchSuggestion;
import com.robot.home.search.vo.HotSearchVO;
import com.robot.home.search.vo.SearchItemVO;
import com.robot.home.search.vo.SearchResultVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一搜索
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    /** Phase11: 搜索关键词最大长度 */
    private static final int MAX_KEYWORD_LENGTH = 100;
    /** Phase11: pageSize上限 */
    private static final int MAX_PAGE_SIZE = 50;

    @Resource
    private SearchService searchService;

    @Resource
    private SearchSuggestionService searchSuggestionService;

    /**
     * 综合搜索（按类型分组）
     */
    @GetMapping
    @RateLimit(action = "search", windowSeconds = 60, maxRequests = 30)
    public Result<SearchResultVO> search(@RequestParam String keyword,
                                         @RequestParam(defaultValue = "5") Integer limit) {
        // Phase11: 关键词长度安全限制
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        Long userId = SecurityUtils.currentUserId();
        SearchResultVO result = searchService.search(keyword, limit);
        searchService.record(keyword, userId);
        // 搜索时增加关键词权重，用于搜索建议排序
        searchSuggestionService.incrementWeight(keyword, null);
        return Result.success(result);
    }

    /**
     * 指定类型分页搜索
     */
    @GetMapping("/{type}")
    @RateLimit(action = "searchByType", windowSeconds = 60, maxRequests = 60)
    public Result<PageResult<SearchItemVO>> searchByType(@PathVariable String type,
                                                         @RequestParam String keyword,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "20") Integer pageSize) {
        // Phase11: 关键词长度安全限制
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        // Phase11: pageSize上限限制
        if (pageSize != null && pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        return Result.success(searchService.searchByType(type, keyword, pageNum, pageSize));
    }

    @GetMapping("/suggest")
    @RateLimit(action = "searchSuggest", windowSeconds = 60, maxRequests = 120)
    public Result<List<String>> suggest(@RequestParam String keyword,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(defaultValue = "10") Integer limit) {
        // Phase11: 关键词长度安全限制
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        // 优先从搜索建议表获取（权重排序），补充原有机器人名称建议
        List<SearchSuggestion> suggestions = searchSuggestionService.suggest(keyword, type, limit);
        List<String> result = suggestions.stream()
                .map(SearchSuggestion::getKeyword)
                .collect(Collectors.toList());
        // 若搜索建议不足，补充原有搜索服务的建议
        if (result.size() < limit) {
            List<String> existing = searchService.suggest(keyword, limit - result.size());
            for (String s : existing) {
                if (!result.contains(s)) {
                    result.add(s);
                    if (result.size() >= limit) break;
                }
            }
        }
        return Result.success(result);
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
