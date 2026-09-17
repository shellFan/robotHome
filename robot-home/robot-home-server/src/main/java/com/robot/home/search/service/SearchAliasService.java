package com.robot.home.search.service;

import com.robot.home.common.PageResult;
import com.robot.home.search.entity.SearchAlias;

import java.util.List;

/**
 * 搜索别名服务
 */
public interface SearchAliasService {

    /** 根据别名查找目标 */
    List<SearchAlias> findByAlias(String alias);

    /** 管理端: 分页列表 */
    PageResult<SearchAlias> adminList(String targetType, Integer pageNum, Integer pageSize);

    /** 管理端: 新增 */
    Long create(SearchAlias alias);

    /** 管理端: 更新 */
    void update(Long id, SearchAlias alias);

    /** 管理端: 启用/停用 */
    void toggleStatus(Long id);
}