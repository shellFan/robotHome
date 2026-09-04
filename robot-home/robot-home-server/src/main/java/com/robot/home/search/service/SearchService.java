package com.robot.home.search.service;

import com.robot.home.common.PageResult;
import com.robot.home.search.vo.HotSearchVO;
import com.robot.home.search.vo.SearchItemVO;
import com.robot.home.search.vo.SearchResultVO;

import java.util.List;

/**
 * 搜索服务抽象：第一版基于数据库 LIKE 实现，
 * 后续可替换为 Elasticsearch 实现而不影响调用方
 */
public interface SearchService {

    /**
     * 综合搜索（按类型分组返回限量结果）
     */
    SearchResultVO search(String keyword, Integer limitPerType);

    /**
     * 指定类型的分页搜索
     */
    PageResult<SearchItemVO> searchByType(String type, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 搜索联想
     */
    List<String> suggest(String keyword, int limit);

    List<HotSearchVO> hotSearches(int limit);

    List<String> history(Long userId, int limit);

    void clearHistory(Long userId);

    /**
     * 记录搜索词（热搜 + 个人历史）
     */
    void record(String keyword, Long userId);
}
