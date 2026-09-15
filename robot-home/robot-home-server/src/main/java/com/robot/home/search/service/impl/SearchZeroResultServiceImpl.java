package com.robot.home.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.search.entity.SearchZeroResult;
import com.robot.home.search.mapper.SearchZeroResultMapper;
import com.robot.home.search.service.SearchZeroResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchZeroResultServiceImpl implements SearchZeroResultService {

    private final SearchZeroResultMapper mapper;

    @Override
    @Transactional
    public void record(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String normalized = keyword.trim().toLowerCase();
        // 尝试更新search_count+1
        int updated = mapper.update(null, new LambdaUpdateWrapper<SearchZeroResult>()
                .eq(SearchZeroResult::getNormalizedKeyword, normalized)
                .setSql("search_count = search_count + 1")
                .set(SearchZeroResult::getLastSearchTime, LocalDateTime.now())
                .set(SearchZeroResult::getUpdateTime, LocalDateTime.now()));
        if (updated == 0) {
            // 首次记录
            SearchZeroResult entity = new SearchZeroResult();
            entity.setNormalizedKeyword(normalized);
            entity.setSearchCount(1);
            entity.setLastSearchTime(LocalDateTime.now());
            entity.setSuggestedAction(null);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            try {
                mapper.insert(entity);
            } catch (DuplicateKeyException e) {
                // 并发uk冲突，再次更新
                mapper.update(null, new LambdaUpdateWrapper<SearchZeroResult>()
                        .eq(SearchZeroResult::getNormalizedKeyword, normalized)
                        .setSql("search_count = search_count + 1")
                        .set(SearchZeroResult::getLastSearchTime, LocalDateTime.now())
                        .set(SearchZeroResult::getUpdateTime, LocalDateTime.now()));
            }
        }
    }

    @Override
    public PageResult<SearchZeroResult> adminList(Integer pageNum, Integer pageSize) {
        pageNum = PageUtils.normalizePageNum(pageNum);
        pageSize = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<SearchZeroResult> wrapper = new LambdaQueryWrapper<SearchZeroResult>()
                .orderByDesc(SearchZeroResult::getSearchCount)
                .orderByDesc(SearchZeroResult::getLastSearchTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SearchZeroResult> zeroPage = mapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(pageNum, pageSize, zeroPage.getTotal(), zeroPage.getRecords());
    }

    @Override
    @Transactional
    public void updateAction(Long id, String action) {
        mapper.update(null, new LambdaUpdateWrapper<SearchZeroResult>()
                .eq(SearchZeroResult::getId, id)
                .set(SearchZeroResult::getSuggestedAction, action)
                .set(SearchZeroResult::getUpdateTime, LocalDateTime.now()));
    }
}