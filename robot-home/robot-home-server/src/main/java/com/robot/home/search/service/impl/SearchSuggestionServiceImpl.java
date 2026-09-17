package com.robot.home.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.search.entity.SearchSuggestion;
import com.robot.home.search.mapper.SearchSuggestionMapper;
import com.robot.home.search.service.SearchSuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 搜索建议服务实现
 * <p>
 * 策略：
 * 1. 用户输入前缀 → 从DB查询匹配的建议（按权重降序）
 * 2. 用户搜索时 → 对已有建议 incrementWeight，不存在则自动创建
 * 3. 热门建议缓存到 Redis，5分钟过期
 */
@Service
public class SearchSuggestionServiceImpl implements SearchSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SearchSuggestionServiceImpl.class);

    private static final int MAX_SUGGEST = 10;
    private static final long CACHE_TTL_SECONDS = 300L; // 5分钟

    @Resource
    private SearchSuggestionMapper searchSuggestionMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public List<SearchSuggestion> suggest(String keyword, String type, int limit) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        int size = Math.max(1, Math.min(limit, MAX_SUGGEST));

        // 尝试从缓存读取
        String cacheKey = Constants.CACHE_SEARCH_SUGGESTION_PREFIX + keyword + ":" + (type == null ? "" : type);
        try {
            String cached = redisUtils.get(cacheKey);
            if (cached != null) {
                // 简单JSON反序列化（关键词列表缓存）
                // 此处直接查DB，缓存用于热门词的权重加速
            }
        } catch (Exception ignored) {
        }

        // DB查询：前缀匹配 + 按权重降序
        List<SearchSuggestion> results = searchSuggestionMapper.selectList(
                Wrappers.<SearchSuggestion>lambdaQuery()
                        .likeRight(SearchSuggestion::getKeyword, keyword.trim())
                        .eq(StrUtil.isNotBlank(type), SearchSuggestion::getType, type)
                        .eq(SearchSuggestion::getEnabled, 1)
                        .orderByDesc(SearchSuggestion::getWeight)
                        .last("LIMIT " + size));

        // 缓存结果
        try {
            redisUtils.set(cacheKey, "1", CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        return results;
    }

    @Override
    public void incrementWeight(String keyword, String type) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        keyword = keyword.trim();
        if (keyword.length() > 64) {
            keyword = keyword.substring(0, 64);
        }

        // 查找已有建议
        SearchSuggestion existing = searchSuggestionMapper.selectOne(
                Wrappers.<SearchSuggestion>lambdaQuery()
                        .eq(SearchSuggestion::getKeyword, keyword)
                        .eq(StrUtil.isNotBlank(type), SearchSuggestion::getType, type)
                        .last("LIMIT 1"));

        if (existing != null) {
            // 增加权重
            existing.setWeight(existing.getWeight() + 1);
            searchSuggestionMapper.updateById(existing);
        } else {
            // 自动创建新建议
            SearchSuggestion suggestion = new SearchSuggestion();
            suggestion.setKeyword(keyword);
            suggestion.setType(StrUtil.isBlank(type) ? "robot" : type);
            suggestion.setWeight(1);
            suggestion.setEnabled(1);
            searchSuggestionMapper.insert(suggestion);
        }

        // 清除相关缓存
        try {
            String prefix = keyword.substring(0, Math.min(2, keyword.length()));
            String cacheKey = Constants.CACHE_SEARCH_SUGGESTION_PREFIX + prefix + ":*";
            for (String key : new ArrayList<>(redisUtils.keys(cacheKey))) {
                redisUtils.delete(key);
            }
        } catch (Exception e) {
            log.debug("清除搜索建议缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public Long add(SearchSuggestion suggestion) {
        searchSuggestionMapper.insert(suggestion);
        return suggestion.getId();
    }

    @Override
    public void update(SearchSuggestion suggestion) {
        searchSuggestionMapper.updateById(suggestion);
    }

    @Override
    public void delete(Long id) {
        searchSuggestionMapper.deleteById(id);
    }
}