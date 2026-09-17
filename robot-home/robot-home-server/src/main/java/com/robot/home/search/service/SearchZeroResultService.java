package com.robot.home.search.service;

import com.robot.home.common.PageResult;
import com.robot.home.search.entity.SearchZeroResult;

/**
 * 零结果搜索服务
 */
public interface SearchZeroResultService {

    /** 记录零结果搜索 */
    void record(String keyword);

    /** 管理端: 分页列表 */
    PageResult<SearchZeroResult> adminList(Integer pageNum, Integer pageSize);

    /** 管理端: 更新建议操作 */
    void updateAction(Long id, String action);
}