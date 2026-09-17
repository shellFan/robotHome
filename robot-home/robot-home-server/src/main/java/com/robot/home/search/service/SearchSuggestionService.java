package com.robot.home.search.service;

import com.robot.home.search.entity.SearchSuggestion;

import java.util.List;

/**
 * 搜索建议服务
 */
public interface SearchSuggestionService {

    /**
     * 根据前缀获取搜索建议（按权重降序）
     *
     * @param keyword 搜索前缀
     * @param type    建议类型（robot/brand/article/tutorial等），null则不限
     * @param limit   最大返回数
     * @return 搜索建议列表
     */
    List<SearchSuggestion> suggest(String keyword, String type, int limit);

    /**
     * 增加搜索关键词的权重（用户搜索时调用）
     *
     * @param keyword 搜索关键词
     * @param type    建议类型
     */
    void incrementWeight(String keyword, String type);

    /**
     * 后台：新增搜索建议
     */
    Long add(SearchSuggestion suggestion);

    /**
     * 后台：更新搜索建议
     */
    void update(SearchSuggestion suggestion);

    /**
     * 后台：删除搜索建议
     */
    void delete(Long id);
}