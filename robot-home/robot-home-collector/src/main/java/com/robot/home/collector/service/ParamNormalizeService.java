package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robot.home.collector.entity.CrawlerParamMapping;
import com.robot.home.collector.mapper.CrawlerParamMappingMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数标准化服务
 * 将原始参数名(raw_name)映射为标准参数名(normalized_name)
 * 如：重量→weight, 身高→height, 自由度→dof
 */
@Service
public class ParamNormalizeService {

    private static final Logger log = LoggerFactory.getLogger(ParamNormalizeService.class);

    @Autowired
    private CrawlerParamMappingMapper paramMappingMapper;

    /** 缓存：rawName → normalizedName */
    private final Map<String, String> mappingCache = new ConcurrentHashMap<>();

    /** 标准参数名集合 */
    private final Set<String> standardParams = new HashSet<>();

    @PostConstruct
    public void init() {
        loadMappings();
    }

    /**
     * 从数据库加载映射
     */
    public void loadMappings() {
        try {
            List<CrawlerParamMapping> mappings = paramMappingMapper.selectList(
                    new LambdaQueryWrapper<CrawlerParamMapping>().eq(CrawlerParamMapping::getIsActive, 1));

            mappingCache.clear();
            standardParams.clear();

            for (CrawlerParamMapping mapping : mappings) {
                // 原始名称→标准名称
                mappingCache.put(mapping.getRawName().toLowerCase(), mapping.getNormalizedName());
                // 别名也加入映射
                if (StringUtils.isNotBlank(mapping.getAliases())) {
                    for (String alias : mapping.getAliases().split(",")) {
                        if (StringUtils.isNotBlank(alias.trim())) {
                            mappingCache.put(alias.trim().toLowerCase(), mapping.getNormalizedName());
                        }
                    }
                }
                standardParams.add(mapping.getNormalizedName());
            }

            log.info("Loaded {} parameter mappings with {} cache entries", mappings.size(), mappingCache.size());
        } catch (Exception e) {
            log.error("Failed to load parameter mappings", e);
        }
    }

    /**
     * 标准化参数名
     * @param rawName 原始参数名（如"重量"、"Weight"、"自重"）
     * @return 标准参数名（如"weight"），未匹配则返回原始名称的小写形式
     */
    public String normalize(String rawName) {
        if (StringUtils.isBlank(rawName)) {
            return rawName;
        }

        String key = rawName.trim().toLowerCase();

        // 精确匹配
        String normalized = mappingCache.get(key);
        if (normalized != null) {
            return normalized;
        }

        // 去除空格/特殊字符后匹配
        String cleaned = key.replaceAll("[\\s\\-_·・：:（）()]", "");
        normalized = mappingCache.get(cleaned);
        if (normalized != null) {
            return normalized;
        }

        // 未匹配，返回小写原始名称
        return key;
    }

    /**
     * 批量标准化参数Map
     * @param rawParams 原始参数Map（key=原始参数名, value=参数值）
     * @return 标准化后的参数Map（key=标准参数名, value=参数值）
     */
    public Map<String, String> normalizeParams(Map<String, String> rawParams) {
        if (rawParams == null || rawParams.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rawParams.entrySet()) {
            String normalizedName = normalize(entry.getKey());
            String value = entry.getValue();

            // 如果已有同名标准参数，追加值（用分号分隔）
            if (normalized.containsKey(normalizedName)) {
                String existing = normalized.get(normalizedName);
                if (!existing.contains(value)) {
                    normalized.put(normalizedName, existing + ";" + value);
                }
            } else {
                normalized.put(normalizedName, value);
            }
        }

        return normalized;
    }

    /**
     * 判断是否为标准参数名
     */
    public boolean isStandardParam(String name) {
        return standardParams.contains(name);
    }

    /**
     * 获取所有标准参数名
     */
    public Set<String> getStandardParams() {
        return Collections.unmodifiableSet(standardParams);
    }

    /**
     * 获取映射缓存大小
     */
    public int getMappingCount() {
        return mappingCache.size();
    }

    /**
     * 添加新映射（运行时动态添加）
     */
    public void addMapping(String rawName, String normalizedName) {
        if (StringUtils.isNotBlank(rawName) && StringUtils.isNotBlank(normalizedName)) {
            mappingCache.put(rawName.trim().toLowerCase(), normalizedName.trim());
        }
    }
}