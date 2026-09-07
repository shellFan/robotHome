package com.robot.home.collector.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 页面分类器
 * 基于URL模式、DOM特征、内容密度等多维度判断页面类型
 * 类型: HOME/LIST/ARTICLE/PRODUCT/CATEGORY/BRAND/COMPANY/OTHER
 */
@Component
public class PageClassifier {

    private static final Logger log = LoggerFactory.getLogger(PageClassifier.class);

    /** 页面类型枚举 */
    public enum PageType {
        HOME,       // 首页
        LIST,       // 列表页（新闻列表、产品列表等）
        ARTICLE,    // 文章详情页
        PRODUCT,    // 产品详情页
        CATEGORY,   // 分类页
        BRAND,      // 品牌页
        COMPANY,    // 公司/关于页
        OTHER       // 其他
    }

    // ===== URL模式 =====

    /** 产品详情URL关键词 */
    private static final Set<String> PRODUCT_URL_KEYWORDS = new HashSet<>(Arrays.asList(
            "/product/", "/products/", "/robot/", "/robots/", "/device/", "/model/",
            "/item/", "/detail/", "/goods/", "/equipment/", "/solution/"
    ));

    /** 产品详情URL正则（含ID模式） */
    private static final Pattern PRODUCT_URL_ID_PATTERN = Pattern.compile(
            "/(product|robot|device|model|item|detail|goods)/[^/]+/?(\\?|$|$)"
    );

    /** 文章详情URL关键词 */
    private static final Set<String> ARTICLE_URL_KEYWORDS = new HashSet<>(Arrays.asList(
            "/news/", "/article/", "/blog/", "/post/", "/press/", "/story/",
            "/content/", "/info/", "/report/", "/update/"
    ));

    /** 列表页URL关键词 */
    private static final Set<String> LIST_URL_KEYWORDS = new HashSet<>(Arrays.asList(
            "/list/", "/category/", "/catalog/", "/search", "/archive/",
            "/page/", "/tag/", "/news", "/articles", "/products"
    ));

    /** 首页URL模式 */
    private static final Pattern HOME_URL_PATTERN = Pattern.compile(
            "^https?://[^/]+/?$"
    );

    /** 分类页URL关键词 */
    private static final Set<String> CATEGORY_URL_KEYWORDS = new HashSet<>(Arrays.asList(
            "/category/", "/categories/", "/class/", "/type/", "/sort/",
            "/filter/", "/industries/", "/application/"
    ));

    /** 品牌页URL关键词 */
    private static final Set<String> BRAND_URL_KEYWORDS = new HashSet<>(Arrays.asList(
            "/brand/", "/brands/", "/manufacturer/", "/maker/"
    ));

    /** 公司页URL关键词 */
    private static final Set<String> COMPANY_URL_KEYWORDS = new HashSet<>(Arrays.asList(
            "/about", "/company/", "/contact", "/team/", "/profile/",
            "/introduction/", "/overview/"
    ));

    // ===== DOM特征 =====

    /** 产品页特征选择器 */
    private static final String[] PRODUCT_SELECTORS = {
            ".product-detail", ".product-info", ".product-spec", ".product-params",
            ".spec-table", ".param-table", ".product-gallery", ".product-description",
            "[itemtype*='Product']", ".price", ".buy-now", ".add-to-cart",
            ".robot-detail", ".robot-spec", ".robot-params"
    };

    /** 文章页特征选择器 */
    private static final String[] ARTICLE_SELECTORS = {
            "article", ".article-content", ".post-content", ".entry-content",
            ".news-content", ".story-body", ".blog-post",
            "[itemtype*='Article']", "time[datetime]", ".author", ".publish-date"
    };

    /** 列表页特征选择器 */
    private static final String[] LIST_SELECTORS = {
            ".product-list", ".news-list", ".article-list", ".item-list",
            ".card-list", ".grid-list", ".list-view", ".search-results",
            "ul.products", ".pagination", ".pager"
    };

    /** 首页特征选择器 */
    private static final String[] HOME_SELECTORS = {
            ".hero", ".banner", ".carousel", ".slider", ".home-section",
            ".featured", ".home-content", "#home", "[class*='home']",
            "[class*='index']", "[class*='main-banner']"
    };

    /**
     * 分类页面
     * @param url 页面URL
     * @param html 页面HTML内容
     * @return 页面类型
     */
    public PageType classify(String url, String html) {
        if (StringUtils.isBlank(url)) {
            return PageType.OTHER;
        }

        String lowerUrl = url.toLowerCase();

        // 1. URL模式匹配（优先级最高）
        PageType urlType = classifyByUrl(lowerUrl);
        if (urlType != null) {
            log.debug("Classified by URL pattern: {} -> {}", url, urlType);
            return urlType;
        }

        // 2. DOM特征匹配
        if (StringUtils.isNotBlank(html)) {
            PageType domType = classifyByDom(html);
            if (domType != null) {
                log.debug("Classified by DOM features: {} -> {}", url, domType);
                return domType;
            }
        }

        // 3. URL深度启发式
        PageType heuristicType = classifyByHeuristic(lowerUrl);
        log.debug("Classified by heuristic: {} -> {}", url, heuristicType);
        return heuristicType;
    }

    /**
     * 基于URL模式分类
     */
    private PageType classifyByUrl(String lowerUrl) {
        // 首页检测
        if (HOME_URL_PATTERN.matcher(lowerUrl).find()) {
            return PageType.HOME;
        }

        // 产品详情页（URL含ID模式优先级高）
        if (PRODUCT_URL_ID_PATTERN.matcher(lowerUrl).find()) {
            return PageType.PRODUCT;
        }
        for (String keyword : PRODUCT_URL_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                // 区分列表和详情：含ID路径的为详情
                String afterKeyword = lowerUrl.substring(lowerUrl.indexOf(keyword) + keyword.length());
                if (afterKeyword.length() > 1 && !afterKeyword.startsWith("?") && !afterKeyword.startsWith("#")) {
                    return PageType.PRODUCT;
                }
            }
        }

        // 文章详情页
        for (String keyword : ARTICLE_URL_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                String afterKeyword = lowerUrl.substring(lowerUrl.indexOf(keyword) + keyword.length());
                if (afterKeyword.length() > 1 && !afterKeyword.startsWith("?") && !afterKeyword.startsWith("#")) {
                    return PageType.ARTICLE;
                }
            }
        }

        // 分类页
        for (String keyword : CATEGORY_URL_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                return PageType.CATEGORY;
            }
        }

        // 品牌页
        for (String keyword : BRAND_URL_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                return PageType.BRAND;
            }
        }

        // 公司页
        for (String keyword : COMPANY_URL_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                return PageType.COMPANY;
            }
        }

        // 列表页
        for (String keyword : LIST_URL_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                return PageType.LIST;
            }
        }

        return null;
    }

    /**
     * 基于DOM特征分类
     */
    private PageType classifyByDom(String html) {
        try {
            Document doc = Jsoup.parse(html);

            // 产品页DOM特征
            int productScore = countMatches(doc, PRODUCT_SELECTORS);
            // 文章页DOM特征
            int articleScore = countMatches(doc, ARTICLE_SELECTORS);
            // 列表页DOM特征
            int listScore = countMatches(doc, LIST_SELECTORS);
            // 首页DOM特征
            int homeScore = countMatches(doc, HOME_SELECTORS);

            // 列表页额外特征：大量链接+分页
            if (hasPagination(doc)) {
                listScore += 3;
            }
            int linkCount = doc.select("a[href]").size();
            if (linkCount > 50) {
                listScore += 2;
            } else if (linkCount > 30) {
                listScore += 1;
            }

            // 产品页额外特征：参数表格
            if (hasSpecTable(doc)) {
                productScore += 3;
            }

            // 文章页额外特征：长文本+少量链接
            String textContent = doc.body() != null ? doc.body().text() : "";
            if (textContent.length() > 500 && linkCount < 20) {
                articleScore += 2;
            }

            // 首页额外特征：banner+多section
            if (doc.select("section, .section").size() > 3) {
                homeScore += 2;
            }

            // 选择最高分
            Map<PageType, Integer> scores = new LinkedHashMap<>();
            scores.put(PageType.PRODUCT, productScore);
            scores.put(PageType.ARTICLE, articleScore);
            scores.put(PageType.LIST, listScore);
            scores.put(PageType.HOME, homeScore);

            PageType bestType = null;
            int bestScore = 2; // 最低阈值
            for (Map.Entry<PageType, Integer> entry : scores.entrySet()) {
                if (entry.getValue() > bestScore) {
                    bestScore = entry.getValue();
                    bestType = entry.getKey();
                }
            }

            return bestType;
        } catch (Exception e) {
            log.debug("DOM classification failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 基于URL深度启发式分类
     */
    private PageType classifyByHeuristic(String lowerUrl) {
        // 计算路径深度
        try {
            String path = lowerUrl.replaceAll("^https?://[^/]+", "").split("\\?")[0].split("#")[0];
            String[] segments = path.split("/");
            int depth = 0;
            for (String seg : segments) {
                if (StringUtils.isNotBlank(seg)) depth++;
            }

            // 深度0-1: 可能是首页
            if (depth <= 1) {
                return PageType.HOME;
            }
            // 深度2-3: 可能是列表或详情
            if (depth == 2) {
                return PageType.LIST;
            }
            // 深度3+: 可能是详情
            return PageType.ARTICLE;
        } catch (Exception e) {
            return PageType.OTHER;
        }
    }

    /**
     * 统计选择器匹配数
     */
    private int countMatches(Document doc, String[] selectors) {
        int count = 0;
        for (String selector : selectors) {
            try {
                Elements elements = doc.select(selector);
                if (!elements.isEmpty()) {
                    count += Math.min(elements.size(), 3); // 单个选择器最多3分
                }
            } catch (Exception ignored) {
            }
        }
        return count;
    }

    /**
     * 检测是否有分页元素
     */
    private boolean hasPagination(Document doc) {
        Elements pagination = doc.select(".pagination, .pager, .page-nav, .page-numbers, " +
                "nav.pagination, ul.page, .load-more, [class*='paginat']");
        return !pagination.isEmpty();
    }

    /**
     * 检测是否有参数表格
     */
    private boolean hasSpecTable(Document doc) {
        Elements specTables = doc.select("table.spec, table.params, table.parameters, " +
                ".spec-table, .param-table, .params-list, .spec-list, " +
                "dl.spec, dl.params, .product-params, .robot-params");
        if (!specTables.isEmpty()) return true;

        // 检测包含参数特征的table
        Elements tables = doc.select("table");
        for (Element table : tables) {
            Elements rows = table.select("tr");
            int paramLikeRows = 0;
            for (Element row : rows) {
                Elements cells = row.select("td, th");
                if (cells.size() == 2) {
                    String key = cells.get(0).text().trim();
                    // 检测常见参数关键词
                    if (key.matches("(?i).*(重量|尺寸|功率|电压|速度|负载|精度|续航|电池|通信|接口|传感器|自由度|臂展|重复定位|防护等级|weight|dimension|power|voltage|speed|payload|accuracy|battery|communication|interface|sensor|DOF|reach|repeatability|IP rating).*")) {
                        paramLikeRows++;
                    }
                }
            }
            if (paramLikeRows >= 2) return true;
        }
        return false;
    }
}