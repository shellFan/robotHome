package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robot.home.collector.entity.CrawlerBrandAlias;
import com.robot.home.collector.mapper.CrawlerBrandAliasMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 品牌匹配服务
 * 通过关键词和别名匹配品牌信息
 * 支持精确匹配、别名匹配、模糊匹配
 */
@Service
public class BrandMatchService {

    private static final Logger log = LoggerFactory.getLogger(BrandMatchService.class);

    @Autowired
    private CrawlerBrandAliasMapper brandAliasMapper;

    /** 品牌名→品牌ID映射 */
    private final Map<String, Long> brandNameToId = new ConcurrentHashMap<>();

    /** 别名→品牌ID映射 */
    private final Map<String, Long> aliasToBrandId = new ConcurrentHashMap<>();

    /** 品牌ID→品牌信息 */
    private final Map<Long, BrandInfo> brandInfoMap = new ConcurrentHashMap<>();

    /** 品牌关键词Pattern列表（用于模糊匹配） */
    private final List<BrandPattern> brandPatterns = new ArrayList<>();

    public static class BrandInfo {
        public Long brandId;
        public String brandName;
        public String officialSite;
        public Set<String> aliases;
    }

    private static class BrandPattern {
        Long brandId;
        Pattern pattern;
        String brandName;
    }

    @PostConstruct
    public void init() {
        loadBrands();
    }

    /**
     * 从数据库加载品牌数据
     */
    public void loadBrands() {
        try {
            List<CrawlerBrandAlias> aliases = brandAliasMapper.selectList(
                    new LambdaQueryWrapper<CrawlerBrandAlias>().eq(CrawlerBrandAlias::getIsActive, 1));

            brandNameToId.clear();
            aliasToBrandId.clear();
            brandInfoMap.clear();
            brandPatterns.clear();

            for (CrawlerBrandAlias alias : aliases) {
                Long brandId = alias.getBrandId();
                String brandName = alias.getBrandName();
                String aliasName = alias.getAliasName();

                // 品牌名→ID
                brandNameToId.put(brandName.toLowerCase(), brandId);

                // 别名→ID
                if (StringUtils.isNotBlank(aliasName)) {
                    aliasToBrandId.put(aliasName.toLowerCase(), brandId);
                }

                // 构建品牌信息
                BrandInfo info = brandInfoMap.computeIfAbsent(brandId, k -> {
                    BrandInfo b = new BrandInfo();
                    b.brandId = brandId;
                    b.brandName = brandName;
                    b.aliases = new HashSet<>();
                    return b;
                });
                if (StringUtils.isNotBlank(aliasName)) {
                    info.aliases.add(aliasName);
                }
            }

            // 构建品牌关键词Pattern
            for (Map.Entry<Long, BrandInfo> entry : brandInfoMap.entrySet()) {
                BrandInfo info = entry.getValue();
                BrandPattern bp = new BrandPattern();
                bp.brandId = info.brandId;
                bp.brandName = info.brandName;
                // 匹配品牌名及其别名
                StringBuilder regex = new StringBuilder();
                regex.append(Pattern.quote(info.brandName));
                for (String alias : info.aliases) {
                    regex.append("|").append(Pattern.quote(alias));
                }
                bp.pattern = Pattern.compile("(?i)\\b(" + regex + ")\\b");
                brandPatterns.add(bp);
            }

            log.info("Loaded {} brands with {} aliases", brandInfoMap.size(), aliases.size());
        } catch (Exception e) {
            log.error("Failed to load brand aliases", e);
        }
    }

    /**
     * 匹配品牌（精确→别名→模糊）
     * @param text 待匹配文本（标题、内容、URL等）
     * @return 匹配结果，可能为null
     */
    public MatchResult matchBrand(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        String lower = text.toLowerCase().trim();

        // 1. 精确匹配品牌名
        for (Map.Entry<String, Long> entry : brandNameToId.entrySet()) {
            if (lower.contains(entry.getKey())) {
                BrandInfo info = brandInfoMap.get(entry.getValue());
                return buildResult(info, "exact");
            }
        }

        // 2. 别名匹配
        for (Map.Entry<String, Long> entry : aliasToBrandId.entrySet()) {
            if (lower.contains(entry.getKey())) {
                BrandInfo info = brandInfoMap.get(entry.getValue());
                return buildResult(info, "alias");
            }
        }

        // 3. 模糊匹配（正则Pattern）
        for (BrandPattern bp : brandPatterns) {
            Matcher m = bp.pattern.matcher(text);
            if (m.find()) {
                BrandInfo info = brandInfoMap.get(bp.brandId);
                return buildResult(info, "fuzzy");
            }
        }

        return null;
    }

    /**
     * 从URL中提取品牌
     */
    public MatchResult matchBrandFromUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }

        // 从域名中匹配
        String domain = extractDomain(url);
        if (domain != null) {
            // 先检查品牌官网
            for (BrandInfo info : brandInfoMap.values()) {
                if (info.officialSite != null && domain.contains(info.officialSite)) {
                    return buildResult(info, "url_domain");
                }
            }
        }

        // 回退到文本匹配
        return matchBrand(url);
    }

    /**
     * 批量匹配品牌（返回最佳匹配）
     */
    public MatchResult matchBest(String... texts) {
        MatchResult best = null;
        int bestScore = 0;

        for (String text : texts) {
            if (StringUtils.isBlank(text)) continue;
            MatchResult result = matchBrand(text);
            if (result != null) {
                int score = getMatchScore(result.matchType);
                if (score > bestScore) {
                    bestScore = score;
                    best = result;
                }
            }
        }

        return best;
    }

    private int getMatchScore(String matchType) {
        switch (matchType) {
            case "exact": return 4;
            case "alias": return 3;
            case "url_domain": return 3;
            case "fuzzy": return 1;
            default: return 0;
        }
    }

    private MatchResult buildResult(BrandInfo info, String matchType) {
        MatchResult result = new MatchResult();
        result.brandId = info.brandId;
        result.brandName = info.brandName;
        result.matchType = matchType;
        result.confidence = getMatchScore(matchType) / 4.0;
        return result;
    }

    private String extractDomain(String url) {
        try {
            if (!url.startsWith("http")) url = "http://" + url;
            java.net.URI uri = new java.net.URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    public static class MatchResult {
        public Long brandId;
        public String brandName;
        public String matchType; // exact, alias, fuzzy, url_domain
        public double confidence; // 0.25~1.0

        @Override
        public String toString() {
            return String.format("MatchResult{brand=%s(id=%d), type=%s, confidence=%.2f}",
                    brandName, brandId, matchType, confidence);
        }
    }
}